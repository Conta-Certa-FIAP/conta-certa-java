package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.BancoDAO;
import br.com.fiap.contacerta.dao.CartaoDAO;
import br.com.fiap.contacerta.dao.ContaBancariaDAO;
import br.com.fiap.contacerta.dao.DividaDAO;
import br.com.fiap.contacerta.dao.ExecutorTransacao;
import br.com.fiap.contacerta.dao.MovimentoCofreDAO;
import br.com.fiap.contacerta.dao.PagamentoDAO;
import br.com.fiap.contacerta.dao.TransacaoDAO;
import br.com.fiap.contacerta.dto.conta.ContaAtualizacaoRequest;
import br.com.fiap.contacerta.dto.conta.ContaRequest;
import br.com.fiap.contacerta.dto.conta.ContaResponse;
import br.com.fiap.contacerta.dto.conta.ImportacaoResumo;
import br.com.fiap.contacerta.exception.RecursoNaoEncontradoException;
import br.com.fiap.contacerta.exception.RegraNegocioException;
import br.com.fiap.contacerta.model.Banco;
import br.com.fiap.contacerta.model.Cartao;
import br.com.fiap.contacerta.model.Categoria;
import br.com.fiap.contacerta.model.ContaBancaria;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.MeioPagamento;
import br.com.fiap.contacerta.model.Transacao;
import br.com.fiap.contacerta.model.Usuario;
import br.com.fiap.contacerta.service.GeradorDadosSimulados.DadosSimulados;
import br.com.fiap.contacerta.service.GeradorDadosSimulados.TransacaoSimulada;

/** Regras das contas bancárias ("conectar um banco" no estilo Open Finance, de forma simulada). */
@Service
public class ContaService {

    private final ContaBancariaDAO contaDAO;
    private final BancoDAO bancoDAO;
    private final CartaoDAO cartaoDAO;
    private final TransacaoDAO transacaoDAO;
    private final DividaDAO dividaDAO;
    private final PagamentoDAO pagamentoDAO;
    private final MovimentoCofreDAO movimentoCofreDAO;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final GeradorDadosSimulados gerador;
    private final ExecutorTransacao executor;
    private final ModelMapper mapper;

    public ContaService(ContaBancariaDAO contaDAO, BancoDAO bancoDAO, CartaoDAO cartaoDAO,
                        TransacaoDAO transacaoDAO, DividaDAO dividaDAO, PagamentoDAO pagamentoDAO,
                        MovimentoCofreDAO movimentoCofreDAO, UsuarioService usuarioService,
                        CategoriaService categoriaService, GeradorDadosSimulados gerador,
                        ExecutorTransacao executor, ModelMapper mapper) {
        this.contaDAO = contaDAO;
        this.bancoDAO = bancoDAO;
        this.cartaoDAO = cartaoDAO;
        this.transacaoDAO = transacaoDAO;
        this.dividaDAO = dividaDAO;
        this.pagamentoDAO = pagamentoDAO;
        this.movimentoCofreDAO = movimentoCofreDAO;
        this.usuarioService = usuarioService;
        this.categoriaService = categoriaService;
        this.gerador = gerador;
        this.executor = executor;
        this.mapper = mapper;
    }

    public List<ContaResponse> listarPorUsuario(Long usuarioId) {
        usuarioService.buscarOuFalhar(usuarioId);
        return contaDAO.listarPorUsuario(usuarioId).stream().map(this::paraResponse).toList();
    }

    public ContaResponse buscar(Long id) {
        return paraResponse(buscarOuFalhar(id));
    }

    public ContaResponse criar(Long usuarioId, ContaRequest request) {
        Usuario usuario = usuarioService.buscarOuFalhar(usuarioId);
        Banco banco = bancoDAO.buscarPorId(request.getBancoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Banco " + request.getBancoId() + " não encontrado."));

        String numeroConta = normalizarNumero(request.getNumeroConta());
        if (contaDAO.existeNumero(banco.getId(), request.getAgencia(), numeroConta, null)) {
            throw new RegraNegocioException("Esta agência e conta já estão cadastradas neste banco.");
        }

        // DTO -> model (copia bancoId, agencia e numeroConta)
        ContaBancaria conta = mapper.map(request, ContaBancaria.class);
        conta.setUsuarioId(usuarioId);
        conta.setBancoNome(banco.getNome());
        conta.setNumeroConta(numeroConta);
        conta.setSaldo(BigDecimal.ZERO.setScale(2));
        conta.setDadosSimulados(request.isImportarDadosSimulados());
        conta.setDataVinculo(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        if (!request.isImportarDadosSimulados()) {
            // Conta "vazia": saldo 0 e o usuário cadastra tudo manualmente.
            executor.executar(con -> {
                contaDAO.inserir(con, conta);
                return null;
            });
            return paraResponse(conta);
        }
        return criarComDadosSimulados(conta, usuario);
    }

    /**
     * Grava a conta e TUDO o que foi simulado (2 cartões, ~40 transações, até 3 dívidas)
     * em UMA transação JDBC: se qualquer INSERT falhar, nada fica gravado pela metade.
     */
    private ContaResponse criarComDadosSimulados(ContaBancaria conta, Usuario usuario) {
        DadosSimulados dados = gerador.gerar(usuario.getRendaDeclarada(), conta.getBancoNome());
        conta.setSaldo(dados.saldoFinal());
        Map<String, Categoria> categorias = categoriaService.mapaPorNome();

        executor.executar(con -> {
            contaDAO.inserir(con, conta); // a partir daqui a conta tem id

            Cartao debito = dados.cartaoDebito();
            debito.setContaId(conta.getId());
            cartaoDAO.inserir(con, debito);

            Cartao credito = dados.cartaoCredito();
            credito.setContaId(conta.getId());
            cartaoDAO.inserir(con, credito);

            List<Transacao> transacoes = dados.transacoes().stream()
                    .map(simulada -> paraTransacao(simulada, conta, debito, credito, categorias))
                    .toList();
            transacaoDAO.inserirEmLote(con, transacoes);

            for (Divida divida : dados.dividas()) {
                divida.setUsuarioId(usuario.getId());
                divida.setContaId(conta.getId());
                dividaDAO.inserir(con, divida);
            }
            return null;
        });

        ContaResponse response = paraResponse(conta);
        response.setImportacao(new ImportacaoResumo(2, dados.transacoes().size(), dados.dividas().size()));
        return response;
    }

    /** Troca o nome da categoria pelo id e liga a transação ao cartão certo. */
    private Transacao paraTransacao(TransacaoSimulada simulada, ContaBancaria conta, Cartao debito, Cartao credito,
                                    Map<String, Categoria> categorias) {
        Categoria categoria = categorias.get(simulada.categoria());
        if (categoria == null) {
            throw new IllegalStateException(
                    "Categoria padrão '" + simulada.categoria() + "' não existe no banco. Rode o database/script.sql.");
        }
        Transacao transacao = new Transacao();
        transacao.setContaId(conta.getId());
        transacao.setCategoriaId(categoria.getId());
        transacao.setDescricao(simulada.descricao());
        transacao.setValor(simulada.valor());
        transacao.setTipo(simulada.tipo());
        transacao.setMeio(simulada.meio());
        transacao.setData(simulada.data());
        transacao.setSimulada(true);
        if (simulada.meio() == MeioPagamento.CARTAO_DEBITO) {
            transacao.setCartaoId(debito.getId());
        } else if (simulada.meio() == MeioPagamento.CARTAO_CREDITO) {
            transacao.setCartaoId(credito.getId());
        }
        return transacao;
    }

    public ContaResponse atualizar(Long id, ContaAtualizacaoRequest request) {
        ContaBancaria conta = buscarOuFalhar(id);
        String numeroConta = normalizarNumero(request.getNumeroConta());
        if (contaDAO.existeNumero(conta.getBancoId(), request.getAgencia(), numeroConta, id)) {
            throw new RegraNegocioException("Esta agência e conta já estão cadastradas neste banco.");
        }
        conta.setAgencia(request.getAgencia());
        conta.setNumeroConta(numeroConta);
        contaDAO.atualizarDados(conta);
        return paraResponse(conta);
    }

    /**
     * Exclui a conta e o que só existe por causa dela (movimentos de cofre, transações e cartões),
     * tudo na mesma transação JDBC e na ordem que respeita as chaves estrangeiras.
     *
     * Decisão do grupo: as DÍVIDAS e os PAGAMENTOS ligados à conta NÃO são apagados, só perdem
     * o vínculo (cd_conta = NULL). Motivo: desconectar o banco do aplicativo não faz a dívida
     * sumir na vida real, e apagar o histórico distorceria a análise financeira do usuário.
     */
    public void remover(Long id) {
        buscarOuFalhar(id);
        executor.executar(con -> {
            movimentoCofreDAO.removerPorConta(con, id);
            transacaoDAO.removerPorConta(con, id); // antes dos cartões: transação aponta para cartão
            cartaoDAO.removerPorConta(con, id);
            pagamentoDAO.desvincularConta(con, id);
            dividaDAO.desvincularConta(con, id);
            contaDAO.remover(con, id);
            return null;
        });
    }

    public ContaBancaria buscarOuFalhar(Long id) {
        return contaDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta " + id + " não encontrada."));
    }

    /** Padroniza o número com hífen antes do dígito: "123456" e "12345-6" viram "12345-6". */
    static String normalizarNumero(String numeroConta) {
        String limpo = numeroConta.trim().toUpperCase().replace("-", "");
        return limpo.substring(0, limpo.length() - 1) + "-" + limpo.substring(limpo.length() - 1);
    }

    private ContaResponse paraResponse(ContaBancaria conta) {
        return mapper.map(conta, ContaResponse.class);
    }
}
