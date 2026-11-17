package br.com.fiap.contacerta.seguranca;

import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.fiap.contacerta.dao.CartaoDAO;
import br.com.fiap.contacerta.dao.CofreDAO;
import br.com.fiap.contacerta.dao.ContaBancariaDAO;
import br.com.fiap.contacerta.dao.DividaDAO;
import br.com.fiap.contacerta.dao.PagamentoDAO;
import br.com.fiap.contacerta.dao.TransacaoDAO;
import br.com.fiap.contacerta.exception.AcessoNegadoException;
import br.com.fiap.contacerta.model.Cofre;
import br.com.fiap.contacerta.model.ContaBancaria;
import br.com.fiap.contacerta.model.Divida;

/**
 * Confere se o registro da rota pertence a quem está logado.
 *
 * Várias rotas recebem um id na URL: /usuarios/{id}/..., /contas/{id}/..., /dividas/{id}...
 * Sem esta conferência, quem estivesse logado poderia trocar o número e ver (ou apagar)
 * os dados de outra pessoa. Aqui descobrimos de quem é o registro e comparamos com o
 * usuário do token; se for de outro, a resposta é 403.
 *
 * Se o registro não existe, deixamos passar: o próprio controller responde 404.
 */
@Component
public class DonoDoRecurso {

    private final ContaBancariaDAO contaDAO;
    private final CartaoDAO cartaoDAO;
    private final TransacaoDAO transacaoDAO;
    private final DividaDAO dividaDAO;
    private final PagamentoDAO pagamentoDAO;
    private final CofreDAO cofreDAO;

    public DonoDoRecurso(ContaBancariaDAO contaDAO, CartaoDAO cartaoDAO, TransacaoDAO transacaoDAO,
                         DividaDAO dividaDAO, PagamentoDAO pagamentoDAO, CofreDAO cofreDAO) {
        this.contaDAO = contaDAO;
        this.cartaoDAO = cartaoDAO;
        this.transacaoDAO = transacaoDAO;
        this.dividaDAO = dividaDAO;
        this.pagamentoDAO = pagamentoDAO;
        this.cofreDAO = cofreDAO;
    }

    /**
     * @param recurso      primeiro trecho da rota: "usuarios", "contas", "dividas"...
     * @param id           o id que veio logo depois dele na URL
     * @param usuarioLogado o id do usuário dono do token
     */
    public void conferir(String recurso, Long id, Long usuarioLogado) {
        Optional<Long> dono = switch (recurso) {
            case "usuarios" -> Optional.of(id);
            case "contas" -> donoDaConta(id);
            case "cartoes" -> cartaoDAO.buscarPorId(id).flatMap(cartao -> donoDaConta(cartao.getContaId()));
            case "transacoes" -> transacaoDAO.buscarPorId(id).flatMap(transacao -> donoDaConta(transacao.getContaId()));
            case "dividas" -> donoDaDivida(id);
            case "pagamentos" -> pagamentoDAO.buscarPorId(id).flatMap(pagamento -> donoDaDivida(pagamento.getDividaId()));
            case "cofres" -> cofreDAO.buscarPorId(id).map(Cofre::getUsuarioId);
            default -> Optional.empty(); // rota sem dono (ex.: /investimentos/...)
        };

        if (dono.isPresent() && !dono.get().equals(usuarioLogado)) {
            throw new AcessoNegadoException();
        }
    }

    private Optional<Long> donoDaConta(Long contaId) {
        return contaDAO.buscarPorId(contaId).map(ContaBancaria::getUsuarioId);
    }

    private Optional<Long> donoDaDivida(Long dividaId) {
        return dividaDAO.buscarPorId(dividaId).map(Divida::getUsuarioId);
    }
}
