package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.CartaoDAO;
import br.com.fiap.contacerta.dao.TransacaoDAO;
import br.com.fiap.contacerta.dto.conta.CartaoAtualizacaoRequest;
import br.com.fiap.contacerta.dto.conta.CartaoRequest;
import br.com.fiap.contacerta.dto.conta.CartaoResponse;
import br.com.fiap.contacerta.exception.RecursoNaoEncontradoException;
import br.com.fiap.contacerta.exception.RegraNegocioException;
import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;
import br.com.fiap.contacerta.model.Cartao;
import br.com.fiap.contacerta.model.TipoCartao;

/** Regras dos cartões de crédito e débito de uma conta. */
@Service
public class CartaoService {

    private final CartaoDAO cartaoDAO;
    private final TransacaoDAO transacaoDAO;
    private final ContaService contaService;
    private final ModelMapper mapper;

    public CartaoService(CartaoDAO cartaoDAO, TransacaoDAO transacaoDAO, ContaService contaService,
                         ModelMapper mapper) {
        this.cartaoDAO = cartaoDAO;
        this.transacaoDAO = transacaoDAO;
        this.contaService = contaService;
        this.mapper = mapper;
    }

    public List<CartaoResponse> listarPorConta(Long contaId) {
        contaService.buscarOuFalhar(contaId);
        return cartaoDAO.listarPorConta(contaId).stream().map(this::paraResponse).toList();
    }

    public CartaoResponse buscar(Long id) {
        return paraResponse(buscarOuFalhar(id));
    }

    public CartaoResponse criar(Long contaId, CartaoRequest request) {
        contaService.buscarOuFalhar(contaId);

        // DTO -> model (o texto "CREDITO"/"DEBITO" vira o enum TipoCartao)
        Cartao cartao = mapper.map(request, Cartao.class);
        cartao.setContaId(contaId);
        cartao.setFaturaAtual(BigDecimal.ZERO.setScale(2));
        cartao.setDataCadastro(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        ajustarPeloTipo(cartao);

        cartaoDAO.inserir(cartao);
        return paraResponse(cartao);
    }

    public CartaoResponse atualizar(Long id, CartaoAtualizacaoRequest request) {
        Cartao cartao = buscarOuFalhar(id);
        cartao.setNumeroFinal(request.getNumeroFinal());
        cartao.setLimite(request.getLimite());
        cartao.setDiaVencimento(request.getDiaVencimento());
        ajustarPeloTipo(cartao);

        if (cartao.getTipo() == TipoCartao.CREDITO && cartao.getLimite().compareTo(cartao.getFaturaAtual()) < 0) {
            throw new RegraNegocioException("O novo limite não pode ser menor que a fatura atual ("
                    + Formatador.reais(cartao.getFaturaAtual()) + ").");
        }
        cartaoDAO.atualizar(cartao);
        return paraResponse(cartao);
    }

    public void remover(Long id) {
        buscarOuFalhar(id);
        if (transacaoDAO.contarPorCartao(id) > 0) {
            throw new RegraNegocioException("Não é possível excluir o cartão: ele já possui transações.");
        }
        cartaoDAO.remover(id);
    }

    public Cartao buscarOuFalhar(Long id) {
        return cartaoDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cartão " + id + " não encontrado."));
    }

    /** Crédito exige limite (400 se faltar). Débito não tem limite nem vencimento: gravamos nulo. */
    private void ajustarPeloTipo(Cartao cartao) {
        if (cartao.getTipo() == TipoCartao.CREDITO) {
            if (cartao.getLimite() == null) {
                throw new RequisicaoInvalidaException("Cartão de crédito exige o campo 'limite'.");
            }
        } else {
            cartao.setLimite(null);
            cartao.setDiaVencimento(null);
        }
    }

    private CartaoResponse paraResponse(Cartao cartao) {
        CartaoResponse response = mapper.map(cartao, CartaoResponse.class);
        response.setLimiteDisponivel(cartao.limiteDisponivel());
        return response;
    }
}
