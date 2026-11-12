package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.ContaBancariaDAO;
import br.com.fiap.contacerta.dao.DividaDAO;
import br.com.fiap.contacerta.dao.ExecutorTransacao;
import br.com.fiap.contacerta.dao.PagamentoDAO;
import br.com.fiap.contacerta.dao.TransacaoDAO;
import br.com.fiap.contacerta.dto.divida.PagamentoRequest;
import br.com.fiap.contacerta.dto.divida.PagamentoResponse;
import br.com.fiap.contacerta.exception.RecursoNaoEncontradoException;
import br.com.fiap.contacerta.exception.RegraNegocioException;
import br.com.fiap.contacerta.exception.SaldoInsuficienteException;
import br.com.fiap.contacerta.model.Categoria;
import br.com.fiap.contacerta.model.CategoriaPadrao;
import br.com.fiap.contacerta.model.ContaBancaria;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.MeioPagamento;
import br.com.fiap.contacerta.model.Pagamento;
import br.com.fiap.contacerta.model.StatusDivida;
import br.com.fiap.contacerta.model.TipoMovimento;
import br.com.fiap.contacerta.model.Transacao;

/**
 * Pagamentos de dívidas.
 *
 * Registrar um pagamento mexe em até 4 tabelas: tb_pagamento (novo registro), tb_divida
 * (saldo, parcelas e status), tb_conta_bancaria (saldo) e tb_transacao (saída no extrato).
 * Por isso tudo roda numa única transação JDBC: ou grava tudo, ou não grava nada.
 */
@Service
public class PagamentoService {

    private final PagamentoDAO pagamentoDAO;
    private final DividaDAO dividaDAO;
    private final ContaBancariaDAO contaDAO;
    private final TransacaoDAO transacaoDAO;
    private final DividaService dividaService;
    private final CategoriaService categoriaService;
    private final ExecutorTransacao executor;
    private final ModelMapper mapper;

    public PagamentoService(PagamentoDAO pagamentoDAO, DividaDAO dividaDAO, ContaBancariaDAO contaDAO,
                            TransacaoDAO transacaoDAO, DividaService dividaService,
                            CategoriaService categoriaService, ExecutorTransacao executor, ModelMapper mapper) {
        this.pagamentoDAO = pagamentoDAO;
        this.dividaDAO = dividaDAO;
        this.contaDAO = contaDAO;
        this.transacaoDAO = transacaoDAO;
        this.dividaService = dividaService;
        this.categoriaService = categoriaService;
        this.executor = executor;
        this.mapper = mapper;
    }

    public List<PagamentoResponse> listarPorDivida(Long dividaId) {
        dividaService.buscarOuFalhar(dividaId);
        return pagamentoDAO.listarPorDivida(dividaId).stream()
                .map(pagamento -> mapper.map(pagamento, PagamentoResponse.class))
                .toList();
    }

    public PagamentoResponse buscar(Long id) {
        Pagamento pagamento = pagamentoDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento " + id + " não encontrado."));
        return mapper.map(pagamento, PagamentoResponse.class);
    }

    public PagamentoResponse registrar(Long dividaId, PagamentoRequest request) {
        BigDecimal valor = request.getValor();
        LocalDate hoje = LocalDate.now();
        LocalDate data = request.getData() != null ? request.getData() : hoje;
        // Buscada ANTES de abrir a transação, para não usar duas conexões do pool ao mesmo tempo.
        Categoria categoria = categoriaService.obrigatoria(CategoriaPadrao.PAGAMENTO_DIVIDAS);

        // DTO -> model (copia valor, contaId e data)
        Pagamento pagamento = mapper.map(request, Pagamento.class);
        pagamento.setDividaId(dividaId);
        pagamento.setData(data);

        Divida atualizada = executor.executar(con -> {
            // FOR UPDATE: trava a dívida (e depois a conta) até o commit.
            Divida divida = dividaDAO.buscarPorIdParaAtualizar(con, dividaId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Dívida " + dividaId + " não encontrada."));

            if (divida.getStatus() == StatusDivida.QUITADA) {
                throw new RegraNegocioException("Esta dívida já está quitada.");
            }
            if (valor.compareTo(divida.getSaldoDevedor()) > 0) {
                throw new RegraNegocioException("O pagamento de " + Formatador.reais(valor)
                        + " é maior que o saldo devedor de " + Formatador.reais(divida.getSaldoDevedor()) + ".");
            }

            ContaBancaria conta = null;
            if (request.getContaId() != null) {
                conta = contaDAO.buscarPorIdParaAtualizar(con, request.getContaId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException(
                                "Conta " + request.getContaId() + " não encontrada."));
                if (!conta.getUsuarioId().equals(divida.getUsuarioId())) {
                    throw new RegraNegocioException("A conta informada pertence a outro usuário.");
                }
                if (valor.compareTo(conta.getSaldo()) > 0) {
                    throw new SaldoInsuficienteException("Saldo insuficiente na conta para este pagamento. Saldo atual: "
                            + Formatador.reais(conta.getSaldo()) + ".");
                }
            }

            aplicarPagamento(divida, valor, hoje);
            dividaDAO.atualizar(con, divida);
            pagamentoDAO.inserir(con, pagamento);

            if (conta != null) {
                contaDAO.somarAoSaldo(con, conta.getId(), valor.negate());
                transacaoDAO.inserir(con, transacaoDoPagamento(divida, conta, categoria, valor, data));
            }
            return divida;
        });

        PagamentoResponse response = mapper.map(pagamento, PagamentoResponse.class);
        response.setDivida(dividaService.paraResponse(atualizada));
        return response;
    }

    /**
     * Atualiza a dívida em memória depois de um pagamento (sem banco: ver PagamentoServiceTest).
     *  - zerou o saldo: vira QUITADA;
     *  - cobriu pelo menos uma parcela: soma nas parcelas pagas, zera o atraso e joga o
     *    vencimento para a frente;
     *  - pagamento menor que uma parcela: só abate o saldo.
     */
    static void aplicarPagamento(Divida divida, BigDecimal valor, LocalDate hoje) {
        BigDecimal novoSaldo = divida.getSaldoDevedor().subtract(valor);
        divida.setSaldoDevedor(novoSaldo);

        if (novoSaldo.signum() == 0) {
            divida.setStatus(StatusDivida.QUITADA);
            divida.setParcelasPagas(divida.getParcelasTotal());
            divida.setDiasAtraso(0);
            divida.setDataProximoVencimento(null);
            return;
        }

        int parcelasCobertas = valor.divideToIntegralValue(divida.getValorParcela()).intValue();
        if (parcelasCobertas >= 1) {
            // Enquanto houver saldo, deixamos pelo menos 1 parcela em aberto.
            int limite = Math.max(divida.getParcelasTotal() - 1, divida.getParcelasPagas());
            divida.setParcelasPagas(Math.min(divida.getParcelasPagas() + parcelasCobertas, limite));
            divida.setDiasAtraso(0);
            divida.setStatus(StatusDivida.EM_DIA);

            if (divida.getDataProximoVencimento() != null) {
                LocalDate vencimento = divida.getDataProximoVencimento().plusMonths(parcelasCobertas);
                while (!vencimento.isAfter(hoje)) { // o próximo vencimento precisa ficar no futuro
                    vencimento = vencimento.plusMonths(1);
                }
                divida.setDataProximoVencimento(vencimento);
            }
        }
    }

    /** A saída que aparece no extrato da conta, na categoria "Pagamento de dívidas". */
    private Transacao transacaoDoPagamento(Divida divida, ContaBancaria conta, Categoria categoria,
                                           BigDecimal valor, LocalDate data) {
        Transacao transacao = new Transacao();
        transacao.setContaId(conta.getId());
        transacao.setCategoriaId(categoria.getId());
        transacao.setDescricao("Pagamento de dívida - " + divida.getCredor());
        transacao.setValor(valor);
        transacao.setTipo(TipoMovimento.SAIDA);
        transacao.setMeio(MeioPagamento.TRANSFERENCIA);
        transacao.setData(data);
        transacao.setSimulada(false);
        return transacao;
    }
}
