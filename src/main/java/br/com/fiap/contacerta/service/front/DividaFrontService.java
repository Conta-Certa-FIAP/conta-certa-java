package br.com.fiap.contacerta.service.front;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.DividaDAO;
import br.com.fiap.contacerta.dao.ExecutorTransacao;
import br.com.fiap.contacerta.dao.PagamentoDAO;
import br.com.fiap.contacerta.dto.divida.PagamentoRequest;
import br.com.fiap.contacerta.dto.front.DebtDetailDto;
import br.com.fiap.contacerta.dto.front.DebtDetailDto.DebtEvent;
import br.com.fiap.contacerta.dto.front.DebtDetailDto.RelatedTrack;
import br.com.fiap.contacerta.dto.front.DebtDto;
import br.com.fiap.contacerta.dto.front.DebtInputDto;
import br.com.fiap.contacerta.dto.front.PaymentInputDto;
import br.com.fiap.contacerta.exception.RecursoNaoEncontradoException;
import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.Pagamento;
import br.com.fiap.contacerta.model.TipoDivida;
import br.com.fiap.contacerta.service.CalculadoraFinanceira;
import br.com.fiap.contacerta.service.DividaService;
import br.com.fiap.contacerta.service.PagamentoService;
import br.com.fiap.contacerta.service.UsuarioService;

/**
 * Dívidas no formato do front (rotas /debts).
 *
 * As regras continuam nos services que já existiam: DividaService.completarCampos calcula
 * status e atraso, DividaService.remover impede apagar dívida com pagamento e
 * PagamentoService.registrar faz o pagamento em transação JDBC. Aqui só traduzimos os campos.
 */
@Service
public class DividaFrontService {

    private static final BigDecimal UM_CENTAVO = new BigDecimal("0.01");

    /**
     * Dívida sem parcelas (rotativo, cheque especial...) em que a pessoa não informou o mínimo:
     * usamos 15% do saldo, o mínimo que os bancos costumam cobrar na fatura do cartão.
     */
    static final BigDecimal MINIMO_SEM_PARCELAS = new BigDecimal("0.15");

    private final DividaDAO dividaDAO;
    private final PagamentoDAO pagamentoDAO;
    private final DividaService dividaService;
    private final PagamentoService pagamentoService;
    private final UsuarioService usuarioService;
    private final ExecutorTransacao executor;

    public DividaFrontService(DividaDAO dividaDAO, PagamentoDAO pagamentoDAO, DividaService dividaService,
                              PagamentoService pagamentoService, UsuarioService usuarioService,
                              ExecutorTransacao executor) {
        this.dividaDAO = dividaDAO;
        this.pagamentoDAO = pagamentoDAO;
        this.dividaService = dividaService;
        this.pagamentoService = pagamentoService;
        this.usuarioService = usuarioService;
        this.executor = executor;
    }

    /** GET /debts: só as dívidas ativas (as quitadas ficam de fora). */
    public List<DebtDto> listar(Long usuarioId) {
        return dividaDAO.listarPorUsuario(usuarioId).stream()
                .filter(Divida::ativa)
                .map(DividaFront::paraDto)
                .toList();
    }

    /** GET /debts/{id}: a dívida com o histórico (mais recente primeiro) e a trilha sugerida. */
    public DebtDetailDto buscar(Long usuarioId, Long id) {
        Divida divida = doUsuario(usuarioId, id);

        List<DebtEvent> historico = new ArrayList<>();
        for (Pagamento pagamento : pagamentoDAO.listarPorDivida(id)) { // o DAO já ordena do mais novo para o mais antigo
            historico.add(new DebtEvent(pagamento.getId(), "PAGAMENTO", pagamento.getData(), pagamento.getValor()));
        }
        // O cadastro é o evento mais antigo. Id 0 para nunca repetir o id de um pagamento.
        historico.add(new DebtEvent(0L, "CADASTRO", divida.getDataContratacao(), divida.getValorOriginal()));

        CatalogoTrilhas.Trilha trilha = CatalogoTrilhas.sugeridaPara(divida.getTipo());
        return DebtDetailDto.de(DividaFront.paraDto(divida), historico, new RelatedTrack(trilha.id(), trilha.titulo()));
    }

    /** POST /debts */
    public DebtDto criar(Long usuarioId, DebtInputDto entrada) {
        usuarioService.buscarOuFalhar(usuarioId);
        LocalDate hoje = LocalDate.now();

        Divida divida = new Divida();
        divida.setUsuarioId(usuarioId);
        divida.setSimulada(false);
        divida.setDataContratacao(hoje);
        preencher(divida, entrada, null);
        divida.setValorOriginal(valorOriginalEstimado(divida));
        DividaService.completarCampos(divida, hoje);

        dividaDAO.inserir(divida);
        return DividaFront.paraDto(divida);
    }

    /**
     * PUT /debts/{id}. Lê e grava a dívida na MESMA transação, com a linha travada (FOR UPDATE):
     * assim um pagamento registrado ao mesmo tempo não é sobrescrito pela edição.
     */
    public DebtDto atualizar(Long usuarioId, Long id, DebtInputDto entrada) {
        Divida atualizada = executor.executar(con -> {
            Divida divida = dividaDAO.buscarPorIdParaAtualizar(con, id)
                    .filter(encontrada -> encontrada.getUsuarioId().equals(usuarioId))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Dívida " + id + " não encontrada."));

            BigDecimal saldoAntes = divida.getSaldoDevedor();
            preencher(divida, entrada, divida.getTipo());
            // Se a pessoa corrigiu o saldo PARA CIMA, o valor original sobe a mesma diferença:
            // o que já foi pago continua contando no progresso.
            BigDecimal aumento = divida.getSaldoDevedor().subtract(saldoAntes).max(BigDecimal.ZERO);
            divida.setValorOriginal(divida.getValorOriginal().add(aumento).max(divida.getSaldoDevedor()));
            divida.setDiasAtraso(null); // nulo = recalcular pelo novo vencimento
            DividaService.completarCampos(divida, LocalDate.now());

            dividaDAO.atualizar(con, divida);
            return divida;
        });
        return DividaFront.paraDto(atualizada);
    }

    /** DELETE /debts/{id}. A regra "dívida com pagamento não pode ser excluída" está no DividaService. */
    public void remover(Long usuarioId, Long id) {
        doUsuario(usuarioId, id);
        dividaService.remover(id);
    }

    /** POST /debts/{id}/payments: registra o pagamento e devolve a dívida atualizada, já com o histórico novo. */
    public DebtDetailDto pagar(Long usuarioId, Long id, PaymentInputDto entrada) {
        doUsuario(usuarioId, id);

        PagamentoRequest pagamento = new PagamentoRequest();
        pagamento.setValor(entrada.amount());
        pagamento.setContaId(entrada.accountId());
        pagamento.setData(entrada.date());
        // O PagamentoService confere saldo, dono da conta e valor maior que a dívida.
        pagamentoService.registrar(id, pagamento);

        return buscar(usuarioId, id);
    }

    /**
     * Valor original de uma dívida nova. O front informa só o saldo de HOJE; se ela é parcelada e
     * já tem parcelas pagas, estimamos o total proporcional (saldo / parcelas que faltam * total),
     * para o progresso começar no ponto certo (ex.: 3 de 12 pagas = 25%).
     */
    static BigDecimal valorOriginalEstimado(Divida divida) {
        int total = divida.getParcelasTotal();
        int pagas = divida.getParcelasPagas();
        if (total <= 1 || pagas == 0 || pagas >= total) {
            return divida.getSaldoDevedor();
        }
        return divida.getSaldoDevedor()
                .multiply(BigDecimal.valueOf(total))
                .divide(BigDecimal.valueOf(total - pagas), 2, RoundingMode.HALF_UP);
    }

    /** Copia para a dívida os campos que vêm do formulário do front. */
    private void preencher(Divida divida, DebtInputDto entrada, TipoDivida tipoAtual) {
        BigDecimal saldo = entrada.balance().setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxa = entrada.monthlyRate().setScale(3, RoundingMode.HALF_UP); // a coluna guarda 3 casas

        // Sem total de parcelas = dívida não parcelada: gravamos como 1 parcela, 0 pagas.
        int total = entrada.installmentsTotal() != null ? entrada.installmentsTotal() : 1;
        int pagas = entrada.installmentsTotal() != null && entrada.installmentsPaid() != null
                ? entrada.installmentsPaid() : 0;
        if (pagas > total) {
            throw new RequisicaoInvalidaException("As parcelas pagas não podem ser mais que o total de parcelas.");
        }

        divida.setNome(entrada.name().trim());
        divida.setCredor(entrada.creditor().trim());
        divida.setTipo(DividaFront.tipoDoRotulo(entrada.type(), tipoAtual));
        divida.setSaldoDevedor(saldo);
        divida.setTaxaJurosMensal(taxa);
        divida.setParcelasTotal(total);
        divida.setParcelasPagas(pagas);
        divida.setValorParcela(parcela(entrada.minimumPayment(), saldo, taxa, total, pagas));
        divida.setDataProximoVencimento(entrada.nextDueDate());
    }

    /**
     * O pagamento mínimo informado. Se a pessoa deixou zero (o banco não aceita parcela zero):
     *  - dívida parcelada: a parcela da Tabela Price sobre o saldo e as parcelas que faltam;
     *  - sem parcelas: 15% do saldo, mas nunca menos que os juros do mês (senão ela só cresce).
     */
    static BigDecimal parcela(BigDecimal minimoInformado, BigDecimal saldo, BigDecimal taxa, int total, int pagas) {
        if (minimoInformado != null && minimoInformado.signum() > 0) {
            return minimoInformado.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal calculada;
        if (total > 1) {
            calculada = CalculadoraFinanceira.parcelaPrice(saldo, taxa, Math.max(1, total - pagas));
        } else {
            BigDecimal quinzePorCento = saldo.multiply(MINIMO_SEM_PARCELAS).setScale(2, RoundingMode.HALF_UP);
            calculada = quinzePorCento.max(CalculadoraFinanceira.jurosDoMes(saldo, taxa)).min(saldo);
        }
        return calculada.max(UM_CENTAVO);
    }

    /** A dívida precisa existir E ser de quem está logado. Nos dois casos de falha a resposta é 404. */
    private Divida doUsuario(Long usuarioId, Long id) {
        return dividaDAO.buscarPorId(id)
                .filter(divida -> divida.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Dívida " + id + " não encontrada."));
    }
}
