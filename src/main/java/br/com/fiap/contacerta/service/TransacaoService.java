package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.CartaoDAO;
import br.com.fiap.contacerta.dao.ContaBancariaDAO;
import br.com.fiap.contacerta.dao.ExecutorTransacao;
import br.com.fiap.contacerta.dao.FiltroTransacao;
import br.com.fiap.contacerta.dao.TransacaoDAO;
import br.com.fiap.contacerta.dto.transacao.SimularCompraRequest;
import br.com.fiap.contacerta.dto.transacao.TransacaoRequest;
import br.com.fiap.contacerta.dto.transacao.TransacaoResponse;
import br.com.fiap.contacerta.exception.LimiteInsuficienteException;
import br.com.fiap.contacerta.exception.RecursoNaoEncontradoException;
import br.com.fiap.contacerta.exception.RegraNegocioException;
import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;
import br.com.fiap.contacerta.exception.SaldoInsuficienteException;
import br.com.fiap.contacerta.model.Cartao;
import br.com.fiap.contacerta.model.Categoria;
import br.com.fiap.contacerta.model.CategoriaPadrao;
import br.com.fiap.contacerta.model.ContaBancaria;
import br.com.fiap.contacerta.model.MeioPagamento;
import br.com.fiap.contacerta.model.TipoCartao;
import br.com.fiap.contacerta.model.TipoMovimento;
import br.com.fiap.contacerta.model.Transacao;

/**
 * Regras das transações (entradas e saídas).
 *
 * Efeito de cada lançamento:
 *  - ENTRADA ...................... soma no saldo da conta;
 *  - SAIDA no PIX/débito/boleto ... tira do saldo (409 se não houver saldo);
 *  - SAIDA no CARTAO_CREDITO ...... soma na fatura do cartão e NÃO mexe no saldo
 *                                   (409 se passar do limite disponível).
 * O lançamento e a atualização do saldo/fatura acontecem na mesma transação JDBC.
 */
@Service
public class TransacaoService {

    private final TransacaoDAO transacaoDAO;
    private final ContaBancariaDAO contaDAO;
    private final CartaoDAO cartaoDAO;
    private final ContaService contaService;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final ExecutorTransacao executor;
    private final ModelMapper mapper;
    private final Random random = new Random();

    public TransacaoService(TransacaoDAO transacaoDAO, ContaBancariaDAO contaDAO, CartaoDAO cartaoDAO,
                            ContaService contaService, UsuarioService usuarioService,
                            CategoriaService categoriaService, ExecutorTransacao executor, ModelMapper mapper) {
        this.transacaoDAO = transacaoDAO;
        this.contaDAO = contaDAO;
        this.cartaoDAO = cartaoDAO;
        this.contaService = contaService;
        this.usuarioService = usuarioService;
        this.categoriaService = categoriaService;
        this.executor = executor;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------ consultas

    public List<TransacaoResponse> listarPorConta(Long contaId, String mes, String tipo, Long categoriaId) {
        contaService.buscarOuFalhar(contaId);
        return listar(montarFiltro(null, contaId, mes, tipo, categoriaId));
    }

    public List<TransacaoResponse> listarPorUsuario(Long usuarioId, String mes, String tipo, Long categoriaId) {
        usuarioService.buscarOuFalhar(usuarioId);
        return listar(montarFiltro(usuarioId, null, mes, tipo, categoriaId));
    }

    public TransacaoResponse buscar(Long id) {
        return paraResponse(buscarOuFalhar(id));
    }

    private List<TransacaoResponse> listar(FiltroTransacao filtro) {
        return transacaoDAO.listar(filtro).stream().map(this::paraResponse).toList();
    }

    /** Converte os parâmetros de texto da URL em um filtro tipado (formato errado -> 400). */
    private FiltroTransacao montarFiltro(Long usuarioId, Long contaId, String mes, String tipo, Long categoriaId) {
        LocalDate inicio = null;
        LocalDate fimExclusivo = null;
        if (mes != null && !mes.isBlank()) {
            Periodo periodo = Periodo.doMes(mes);
            inicio = periodo.inicio();
            fimExclusivo = periodo.fimExclusivo();
        }
        TipoMovimento tipoMovimento = null;
        if (tipo != null && !tipo.isBlank()) {
            try {
                tipoMovimento = TipoMovimento.valueOf(tipo.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RequisicaoInvalidaException("O parâmetro 'tipo' deve ser ENTRADA ou SAIDA.");
            }
        }
        return new FiltroTransacao(usuarioId, contaId, inicio, fimExclusivo, tipoMovimento, categoriaId);
    }

    // ------------------------------------------------------------------ lançamento manual

    public TransacaoResponse lancar(Long contaId, TransacaoRequest request) {
        Categoria categoria = categoriaService.buscarOuFalhar(request.getCategoriaId());

        // DTO -> model (os textos de tipo e meio viram enums)
        Transacao transacao = mapper.map(request, Transacao.class);
        transacao.setContaId(contaId);
        transacao.setCategoriaNome(categoria.getNome());
        transacao.setDescricao(request.getDescricao().trim());
        transacao.setData(request.getData() != null ? request.getData() : LocalDate.now());
        transacao.setSimulada(false);

        validarLancamento(transacao, categoria);
        return paraResponse(gravar(transacao));
    }

    private void validarLancamento(Transacao transacao, Categoria categoria) {
        if (categoria.getTipo() != transacao.getTipo()) {
            throw new RequisicaoInvalidaException("A categoria '" + categoria.getNome() + "' é de "
                    + categoria.getTipo() + ", mas a transação foi enviada como " + transacao.getTipo() + ".");
        }
        if (CategoriaPadrao.deSistema(categoria.getNome())) {
            throw new RegraNegocioException("A categoria '" + categoria.getNome()
                    + "' é preenchida automaticamente. Use as rotas de pagamento de dívida ou do cofre.");
        }
        boolean usaCartao = transacao.getMeio().usaCartao();
        if (transacao.getTipo() == TipoMovimento.ENTRADA && usaCartao) {
            throw new RequisicaoInvalidaException("Uma entrada não pode ter cartão como meio. Use PIX, TRANSFERENCIA ou OUTRO.");
        }
        if (usaCartao && transacao.getCartaoId() == null) {
            throw new RequisicaoInvalidaException("Informe o 'cartaoId' quando o meio for cartão.");
        }
        if (!usaCartao && transacao.getCartaoId() != null) {
            throw new RequisicaoInvalidaException("O 'cartaoId' só deve ser enviado quando o meio for cartão.");
        }
    }

    // ------------------------------------------------------------------ simular compra

    /**
     * Botão "simular compra" do front: cria uma compra realista no cartão de crédito ou débito
     * da conta. Valor e categoria são sorteados, a menos que venham no corpo.
     */
    public TransacaoResponse simularCompra(Long contaId, SimularCompraRequest request) {
        TipoCartao tipoCartao = TipoCartao.valueOf(request.getTipoCartao());
        Categoria categoria = escolherCategoriaDaCompra(request.getCategoriaId());

        String descricao;
        BigDecimal valor;
        if (CatalogoCompras.temCategoria(categoria.getNome())) {
            CatalogoCompras.ModeloCompra modelo = CatalogoCompras.sortear(categoria.getNome(), random);
            descricao = modelo.descricao();
            valor = CatalogoCompras.sortearValor(modelo, random);
        } else {
            descricao = "Compra em " + categoria.getNome();
            valor = BigDecimal.valueOf(20 + random.nextInt(131)).setScale(2);
        }
        if (request.getValor() != null) {
            valor = request.getValor();
        }

        Transacao transacao = new Transacao();
        transacao.setContaId(contaId);
        transacao.setCategoriaId(categoria.getId());
        transacao.setCategoriaNome(categoria.getNome());
        transacao.setDescricao(descricao);
        transacao.setValor(valor);
        transacao.setTipo(TipoMovimento.SAIDA);
        transacao.setMeio(tipoCartao == TipoCartao.CREDITO ? MeioPagamento.CARTAO_CREDITO : MeioPagamento.CARTAO_DEBITO);
        transacao.setData(LocalDate.now());
        transacao.setSimulada(true);

        Transacao gravada = executor.executar(con -> {
            ContaBancaria conta = travarConta(con, contaId);
            Cartao cartao = cartaoDAO.buscarPorContaETipo(con, contaId, tipoCartao)
                    .orElseThrow(() -> new RegraNegocioException(
                            "Esta conta não tem cartão de " + tipoCartao.getDescricao() + " cadastrado."));
            transacao.setCartaoId(cartao.getId());
            aplicarNoSaldoOuNaFatura(con, conta, transacao);
            transacaoDAO.inserir(con, transacao);
            return transacao;
        });
        return paraResponse(gravada);
    }

    private Categoria escolherCategoriaDaCompra(Long categoriaId) {
        if (categoriaId == null) {
            String sorteada = CatalogoCompras.CATEGORIAS.get(random.nextInt(CatalogoCompras.CATEGORIAS.size()));
            return categoriaService.obrigatoria(sorteada);
        }
        Categoria categoria = categoriaService.buscarOuFalhar(categoriaId);
        if (categoria.getTipo() != TipoMovimento.SAIDA) {
            throw new RequisicaoInvalidaException("Uma compra precisa de uma categoria de SAIDA.");
        }
        if (CategoriaPadrao.deSistema(categoria.getNome())) {
            throw new RegraNegocioException("A categoria '" + categoria.getNome() + "' não pode ser usada em compras.");
        }
        return categoria;
    }

    // ------------------------------------------------------------------ exclusão

    /** Exclui a transação DESFAZENDO o efeito dela no saldo ou na fatura. */
    public void remover(Long id) {
        Transacao transacao = buscarOuFalhar(id);
        if (CategoriaPadrao.deSistema(transacao.getCategoriaNome())) {
            throw new RegraNegocioException("Esta transação foi criada por um pagamento de dívida ou movimento"
                    + " de cofre e não pode ser excluída diretamente.");
        }
        executor.executar(con -> {
            ContaBancaria conta = travarConta(con, transacao.getContaId());
            if (transacao.getTipo() == TipoMovimento.ENTRADA) {
                if (conta.getSaldo().compareTo(transacao.getValor()) < 0) {
                    throw new SaldoInsuficienteException(
                            "Não é possível excluir esta entrada: o saldo da conta ficaria negativo.");
                }
                contaDAO.somarAoSaldo(con, conta.getId(), transacao.getValor().negate());
            } else if (transacao.getMeio() == MeioPagamento.CARTAO_CREDITO) {
                cartaoDAO.somarNaFatura(con, transacao.getCartaoId(), transacao.getValor().negate());
            } else {
                contaDAO.somarAoSaldo(con, conta.getId(), transacao.getValor());
            }
            transacaoDAO.remover(con, id);
            return null;
        });
    }

    // ------------------------------------------------------------------ apoio

    private Transacao gravar(Transacao transacao) {
        return executor.executar(con -> {
            ContaBancaria conta = travarConta(con, transacao.getContaId());
            aplicarNoSaldoOuNaFatura(con, conta, transacao);
            transacaoDAO.inserir(con, transacao);
            return transacao;
        });
    }

    private ContaBancaria travarConta(Connection con, Long contaId) throws SQLException {
        return contaDAO.buscarPorIdParaAtualizar(con, contaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta " + contaId + " não encontrada."));
    }

    /** Aplica o efeito financeiro do lançamento. Qualquer exceção aqui causa rollback da transação JDBC. */
    private void aplicarNoSaldoOuNaFatura(Connection con, ContaBancaria conta, Transacao transacao)
            throws SQLException {
        BigDecimal valor = transacao.getValor();

        if (transacao.getTipo() == TipoMovimento.ENTRADA) {
            contaDAO.somarAoSaldo(con, conta.getId(), valor);
            return;
        }

        if (transacao.getMeio() == MeioPagamento.CARTAO_CREDITO) {
            Cartao cartao = buscarCartaoDaConta(con, transacao.getCartaoId(), conta.getId(), TipoCartao.CREDITO);
            BigDecimal disponivel = cartao.limiteDisponivel();
            if (valor.compareTo(disponivel) > 0) {
                throw new LimiteInsuficienteException("Limite insuficiente no cartão de crédito final "
                        + cartao.getNumeroFinal() + ". Disponível: " + Formatador.reais(disponivel) + ".");
            }
            cartaoDAO.somarNaFatura(con, cartao.getId(), valor);
            return;
        }

        if (transacao.getMeio() == MeioPagamento.CARTAO_DEBITO) {
            buscarCartaoDaConta(con, transacao.getCartaoId(), conta.getId(), TipoCartao.DEBITO);
        }
        if (valor.compareTo(conta.getSaldo()) > 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente na conta. Saldo atual: " + Formatador.reais(conta.getSaldo()) + ".");
        }
        contaDAO.somarAoSaldo(con, conta.getId(), valor.negate());
    }

    /** O cartão precisa existir (404), ser desta conta (409) e do tipo que combina com o meio (400). */
    private Cartao buscarCartaoDaConta(Connection con, Long cartaoId, Long contaId, TipoCartao tipoEsperado)
            throws SQLException {
        Cartao cartao = cartaoDAO.buscarPorIdParaAtualizar(con, cartaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cartão " + cartaoId + " não encontrado."));
        if (!cartao.getContaId().equals(contaId)) {
            throw new RegraNegocioException("O cartão informado não pertence a esta conta.");
        }
        if (cartao.getTipo() != tipoEsperado) {
            throw new RequisicaoInvalidaException("O cartão informado é de " + cartao.getTipo().getDescricao()
                    + ", mas o meio escolhido exige cartão de " + tipoEsperado.getDescricao() + ".");
        }
        return cartao;
    }

    private Transacao buscarOuFalhar(Long id) {
        return transacaoDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transação " + id + " não encontrada."));
    }

    private TransacaoResponse paraResponse(Transacao transacao) {
        return mapper.map(transacao, TransacaoResponse.class);
    }
}
