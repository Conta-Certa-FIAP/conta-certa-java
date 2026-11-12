package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.CofreDAO;
import br.com.fiap.contacerta.dao.ContaBancariaDAO;
import br.com.fiap.contacerta.dao.ExecutorTransacao;
import br.com.fiap.contacerta.dao.MovimentoCofreDAO;
import br.com.fiap.contacerta.dao.TransacaoDAO;
import br.com.fiap.contacerta.dto.cofre.CofreRequest;
import br.com.fiap.contacerta.dto.cofre.CofreResponse;
import br.com.fiap.contacerta.dto.cofre.MovimentoCofreRequest;
import br.com.fiap.contacerta.dto.cofre.MovimentoCofreResponse;
import br.com.fiap.contacerta.exception.RecursoNaoEncontradoException;
import br.com.fiap.contacerta.exception.RegraNegocioException;
import br.com.fiap.contacerta.exception.SaldoInsuficienteException;
import br.com.fiap.contacerta.model.Categoria;
import br.com.fiap.contacerta.model.CategoriaPadrao;
import br.com.fiap.contacerta.model.Cofre;
import br.com.fiap.contacerta.model.ContaBancaria;
import br.com.fiap.contacerta.model.MeioPagamento;
import br.com.fiap.contacerta.model.MovimentoCofre;
import br.com.fiap.contacerta.model.TipoMovimento;
import br.com.fiap.contacerta.model.TipoMovimentoCofre;
import br.com.fiap.contacerta.model.Transacao;

/**
 * Cofres: lugares para guardar dinheiro com uma meta.
 *
 * Depósito e resgate mexem em 4 tabelas (conta, cofre, movimento do cofre e transação do
 * extrato), então rodam em uma única transação JDBC.
 */
@Service
public class CofreService {

    private final CofreDAO cofreDAO;
    private final MovimentoCofreDAO movimentoDAO;
    private final ContaBancariaDAO contaDAO;
    private final TransacaoDAO transacaoDAO;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final ExecutorTransacao executor;
    private final ModelMapper mapper;

    public CofreService(CofreDAO cofreDAO, MovimentoCofreDAO movimentoDAO, ContaBancariaDAO contaDAO,
                        TransacaoDAO transacaoDAO, UsuarioService usuarioService,
                        CategoriaService categoriaService, ExecutorTransacao executor, ModelMapper mapper) {
        this.cofreDAO = cofreDAO;
        this.movimentoDAO = movimentoDAO;
        this.contaDAO = contaDAO;
        this.transacaoDAO = transacaoDAO;
        this.usuarioService = usuarioService;
        this.categoriaService = categoriaService;
        this.executor = executor;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------ CRUD

    public List<CofreResponse> listarPorUsuario(Long usuarioId) {
        usuarioService.buscarOuFalhar(usuarioId);
        return cofreDAO.listarPorUsuario(usuarioId).stream().map(this::paraResponse).toList();
    }

    public CofreResponse buscar(Long id) {
        return paraResponse(buscarOuFalhar(id));
    }

    public CofreResponse criar(Long usuarioId, CofreRequest request) {
        usuarioService.buscarOuFalhar(usuarioId);

        // DTO -> model (copia nome, valorMeta e dataLimite)
        Cofre cofre = mapper.map(request, Cofre.class);
        cofre.setUsuarioId(usuarioId);
        cofre.setNome(request.getNome().trim());
        cofre.setValorGuardado(BigDecimal.ZERO.setScale(2));
        cofre.setDataCriacao(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        cofreDAO.inserir(cofre);
        return paraResponse(cofre);
    }

    public CofreResponse atualizar(Long id, CofreRequest request) {
        Cofre cofre = buscarOuFalhar(id);
        cofre.setNome(request.getNome().trim());
        cofre.setValorMeta(request.getValorMeta());
        cofre.setDataLimite(request.getDataLimite());
        cofreDAO.atualizar(cofre);
        return paraResponse(cofre);
    }

    /** Cofre com dinheiro não pode ser excluído: é preciso resgatar antes (409). */
    public void remover(Long id) {
        Cofre cofre = buscarOuFalhar(id);
        if (cofre.getValorGuardado().signum() > 0) {
            throw new RegraNegocioException("Este cofre ainda guarda " + Formatador.reais(cofre.getValorGuardado())
                    + ". Resgate o valor antes de excluir.");
        }
        // O histórico de movimentos é filho do cofre: apagamos os dois juntos.
        executor.executar(con -> {
            movimentoDAO.removerPorCofre(con, id);
            cofreDAO.remover(con, id);
            return null;
        });
    }

    // ------------------------------------------------------------------ movimentos

    public List<MovimentoCofreResponse> listarMovimentos(Long cofreId) {
        buscarOuFalhar(cofreId);
        return movimentoDAO.listarPorCofre(cofreId).stream()
                .map(movimento -> mapper.map(movimento, MovimentoCofreResponse.class))
                .toList();
    }

    public MovimentoCofreResponse buscarMovimento(Long cofreId, Long movimentoId) {
        buscarOuFalhar(cofreId);
        MovimentoCofre movimento = movimentoDAO.buscarPorId(movimentoId)
                .filter(m -> m.getCofreId().equals(cofreId))
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Movimento " + movimentoId + " não encontrado neste cofre."));
        return mapper.map(movimento, MovimentoCofreResponse.class);
    }

    /** Depósito: tira do saldo da conta e soma no cofre. */
    public MovimentoCofreResponse depositar(Long cofreId, MovimentoCofreRequest request) {
        return movimentar(cofreId, request, TipoMovimentoCofre.DEPOSITO);
    }

    /** Resgate: tira do cofre e devolve para a conta. */
    public MovimentoCofreResponse resgatar(Long cofreId, MovimentoCofreRequest request) {
        return movimentar(cofreId, request, TipoMovimentoCofre.RESGATE);
    }

    private MovimentoCofreResponse movimentar(Long cofreId, MovimentoCofreRequest request, TipoMovimentoCofre tipo) {
        boolean deposito = tipo == TipoMovimentoCofre.DEPOSITO;
        BigDecimal valor = request.getValor();
        // Buscada ANTES de abrir a transação, para não usar duas conexões do pool ao mesmo tempo.
        Categoria categoria = categoriaService.obrigatoria(
                deposito ? CategoriaPadrao.DEPOSITO_COFRE : CategoriaPadrao.RESGATE_COFRE);

        // DTO -> model (copia contaId e valor)
        MovimentoCofre movimento = mapper.map(request, MovimentoCofre.class);
        movimento.setCofreId(cofreId);
        movimento.setTipo(tipo);
        movimento.setData(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        Cofre atualizado = executor.executar(con -> {
            // FOR UPDATE: trava o cofre e a conta até o commit.
            Cofre cofre = cofreDAO.buscarPorIdParaAtualizar(con, cofreId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Cofre " + cofreId + " não encontrado."));
            ContaBancaria conta = contaDAO.buscarPorIdParaAtualizar(con, request.getContaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Conta " + request.getContaId() + " não encontrada."));

            if (!conta.getUsuarioId().equals(cofre.getUsuarioId())) {
                throw new RegraNegocioException("A conta informada pertence a outro usuário.");
            }
            if (deposito && valor.compareTo(conta.getSaldo()) > 0) {
                throw new SaldoInsuficienteException("Saldo insuficiente na conta para este depósito. Saldo atual: "
                        + Formatador.reais(conta.getSaldo()) + ".");
            }
            if (!deposito && valor.compareTo(cofre.getValorGuardado()) > 0) {
                throw new RegraNegocioException("O resgate de " + Formatador.reais(valor)
                        + " é maior que o valor guardado (" + Formatador.reais(cofre.getValorGuardado()) + ").");
            }

            // Depósito: conta -valor, cofre +valor. Resgate: o contrário.
            BigDecimal variacaoDoCofre = deposito ? valor : valor.negate();
            contaDAO.somarAoSaldo(con, conta.getId(), variacaoDoCofre.negate());
            cofreDAO.somarAoGuardado(con, cofre.getId(), variacaoDoCofre);
            movimentoDAO.inserir(con, movimento);
            transacaoDAO.inserir(con, transacaoDoMovimento(cofre, conta, categoria, valor, deposito));

            cofre.setValorGuardado(cofre.getValorGuardado().add(variacaoDoCofre));
            return cofre;
        });

        MovimentoCofreResponse response = mapper.map(movimento, MovimentoCofreResponse.class);
        response.setCofre(paraResponse(atualizado));
        return response;
    }

    /** A linha que aparece no extrato: SAIDA "Depósito no cofre" ou ENTRADA "Resgate do cofre". */
    private Transacao transacaoDoMovimento(Cofre cofre, ContaBancaria conta, Categoria categoria,
                                           BigDecimal valor, boolean deposito) {
        Transacao transacao = new Transacao();
        transacao.setContaId(conta.getId());
        transacao.setCategoriaId(categoria.getId());
        transacao.setDescricao((deposito ? "Depósito no cofre - " : "Resgate do cofre - ") + cofre.getNome());
        transacao.setValor(valor);
        transacao.setTipo(deposito ? TipoMovimento.SAIDA : TipoMovimento.ENTRADA);
        transacao.setMeio(MeioPagamento.TRANSFERENCIA);
        transacao.setData(LocalDate.now());
        transacao.setSimulada(false);
        return transacao;
    }

    // ------------------------------------------------------------------ metas

    /** Progresso da meta de um cofre. Campos nulos quando não se aplicam. */
    record ProgressoMeta(BigDecimal percentualConcluido, BigDecimal valorFaltante, Integer mesesRestantes,
                         BigDecimal valorMensalNecessario, boolean metaAtingida, boolean prazoVencido) {
    }

    /**
     * Calcula o progresso da meta (sem banco de dados: ver CofreServiceTest).
     *   % concluído       = guardado / meta (no máximo 100%)
     *   quanto falta      = meta - guardado (no mínimo 0)
     *   guardar por mês   = quanto falta / meses até a data limite (arredondando os meses e os
     *                       centavos para cima, para a pessoa chegar na meta dentro do prazo)
     */
    static ProgressoMeta calcularProgresso(Cofre cofre, LocalDate hoje) {
        if (cofre.getValorMeta() == null) {
            return new ProgressoMeta(null, null, null, null, false, false);
        }
        BigDecimal meta = cofre.getValorMeta();
        BigDecimal guardado = cofre.getValorGuardado();
        BigDecimal faltante = meta.subtract(guardado).max(BigDecimal.ZERO);
        BigDecimal percentual = guardado.multiply(BigDecimal.valueOf(100))
                .divide(meta, 2, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100).setScale(2));
        boolean atingida = faltante.signum() == 0;

        if (atingida || cofre.getDataLimite() == null) {
            return new ProgressoMeta(percentual, faltante, null, null, atingida, false);
        }
        if (!cofre.getDataLimite().isAfter(hoje)) {
            return new ProgressoMeta(percentual, faltante, 0, null, false, true);
        }

        // Meses inteiros até o prazo; se sobrar um "pedaço" de mês, conta mais um.
        long meses = ChronoUnit.MONTHS.between(hoje, cofre.getDataLimite());
        if (hoje.plusMonths(meses).isBefore(cofre.getDataLimite())) {
            meses++;
        }
        meses = Math.max(1, meses);
        BigDecimal porMes = faltante.divide(BigDecimal.valueOf(meses), 2, RoundingMode.CEILING);
        return new ProgressoMeta(percentual, faltante, (int) meses, porMes, false, false);
    }

    // ------------------------------------------------------------------ apoio

    public Cofre buscarOuFalhar(Long id) {
        return cofreDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cofre " + id + " não encontrado."));
    }

    /** model -> DTO de saída, acrescentando o progresso da meta. */
    private CofreResponse paraResponse(Cofre cofre) {
        CofreResponse response = mapper.map(cofre, CofreResponse.class);
        ProgressoMeta progresso = calcularProgresso(cofre, LocalDate.now());
        response.setPercentualConcluido(progresso.percentualConcluido());
        response.setValorFaltante(progresso.valorFaltante());
        response.setMesesRestantes(progresso.mesesRestantes());
        response.setValorMensalNecessario(progresso.valorMensalNecessario());
        response.setMetaAtingida(progresso.metaAtingida());
        response.setPrazoVencido(progresso.prazoVencido());
        return response;
    }
}
