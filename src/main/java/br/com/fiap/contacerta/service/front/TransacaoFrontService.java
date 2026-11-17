package br.com.fiap.contacerta.service.front;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.ContaBancariaDAO;
import br.com.fiap.contacerta.dao.FiltroTransacao;
import br.com.fiap.contacerta.dao.TransacaoDAO;
import br.com.fiap.contacerta.dto.front.NewTransactionDto;
import br.com.fiap.contacerta.dto.front.TransactionDto;
import br.com.fiap.contacerta.dto.transacao.TransacaoRequest;
import br.com.fiap.contacerta.dto.transacao.TransacaoResponse;
import br.com.fiap.contacerta.exception.RecursoNaoEncontradoException;
import br.com.fiap.contacerta.exception.RegraNegocioException;
import br.com.fiap.contacerta.model.Categoria;
import br.com.fiap.contacerta.model.ContaBancaria;
import br.com.fiap.contacerta.model.MeioPagamento;
import br.com.fiap.contacerta.model.TipoMovimento;
import br.com.fiap.contacerta.model.Transacao;
import br.com.fiap.contacerta.service.CategoriaService;
import br.com.fiap.contacerta.service.Periodo;
import br.com.fiap.contacerta.service.TransacaoService;

/**
 * Transações no formato do front (rotas /transactions).
 *
 * O lançamento passa pelo TransacaoService que já existia: ele confere o saldo, atualiza a conta
 * e grava tudo na mesma transação JDBC. Aqui só escolhemos a conta e traduzimos a categoria.
 */
@Service
public class TransacaoFrontService {

    private final TransacaoDAO transacaoDAO;
    private final ContaBancariaDAO contaDAO;
    private final CategoriaService categoriaService;
    private final TransacaoService transacaoService;

    public TransacaoFrontService(TransacaoDAO transacaoDAO, ContaBancariaDAO contaDAO,
                                 CategoriaService categoriaService, TransacaoService transacaoService) {
        this.transacaoDAO = transacaoDAO;
        this.contaDAO = contaDAO;
        this.categoriaService = categoriaService;
        this.transacaoService = transacaoService;
    }

    /** GET /transactions?month=AAAA-MM: todas as contas da pessoa, do mais recente para o mais antigo. */
    public List<TransactionDto> listar(Long usuarioId, String month) {
        Periodo periodo = Periodo.doMes(MesFront.ler(month));
        FiltroTransacao filtro = new FiltroTransacao(usuarioId, null, periodo.inicio(), periodo.fimExclusivo(), null, null);
        return transacaoDAO.listar(filtro).stream().map(TransacaoFrontService::paraDto).toList();
    }

    /** POST /transactions */
    public TransactionDto criar(Long usuarioId, NewTransactionDto entrada) {
        ContaBancaria conta = escolherConta(usuarioId, entrada.accountId());
        TipoMovimento tipo = TipoMovimento.valueOf(entrada.type());
        Categoria categoria = categoriaService.obrigatoria(CategoriaFront.nomeNoBanco(tipo, entrada.category()));

        TransacaoRequest request = new TransacaoRequest();
        request.setCategoriaId(categoria.getId());
        request.setDescricao(entrada.description().trim());
        request.setValor(entrada.amount());
        request.setTipo(tipo.name());
        // O formulário do front não pergunta o meio; PIX é o que mexe direto no saldo da conta.
        request.setMeio(MeioPagamento.PIX.name());
        request.setData(entrada.date());
        request.setRecorrente(Boolean.TRUE.equals(entrada.recurring()));

        TransacaoResponse criada = transacaoService.lancar(conta.getId(), request);
        return new TransactionDto(criada.getId(), criada.getTipo(), criada.getDescricao(),
                CategoriaFront.codigo(criada.getCategoriaNome()), criada.getValor(), criada.getData(),
                criada.isRecorrente());
    }

    /**
     * A conta informada (precisa ser da pessoa) ou, se não vier, a primeira conta cadastrada.
     * Sem nenhuma conta não há onde lançar: a pessoa precisa conectar um banco antes.
     */
    private ContaBancaria escolherConta(Long usuarioId, Long contaId) {
        if (contaId != null) {
            // Conta de outra pessoa responde igual a conta inexistente (404): não revelamos que ela existe.
            return contaDAO.buscarPorId(contaId)
                    .filter(conta -> conta.getUsuarioId().equals(usuarioId))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Conta " + contaId + " não encontrada."));
        }
        return contaDAO.listarPorUsuario(usuarioId).stream().findFirst()
                .orElseThrow(() -> new RegraNegocioException(
                        "Você ainda não tem uma conta bancária. Conecte um banco em \"Contas\" antes de lançar transações."));
    }

    static TransactionDto paraDto(Transacao transacao) {
        return new TransactionDto(transacao.getId(), transacao.getTipo().name(), transacao.getDescricao(),
                CategoriaFront.codigo(transacao.getCategoriaNome()), transacao.getValor(), transacao.getData(),
                transacao.isRecorrente());
    }
}
