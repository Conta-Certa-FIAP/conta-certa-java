package br.com.fiap.contacerta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.StatusDivida;
import br.com.fiap.contacerta.model.TipoDivida;

/** Regras de dívida e pagamento que não dependem do banco de dados. */
class PagamentoServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 11);

    private Divida dividaAtrasada() {
        Divida divida = new Divida();
        divida.setTipo(TipoDivida.EMPRESTIMO_PESSOAL);
        divida.setValorOriginal(new BigDecimal("1200.00"));
        divida.setSaldoDevedor(new BigDecimal("1000.00"));
        divida.setTaxaJurosMensal(new BigDecimal("5"));
        divida.setParcelasTotal(12);
        divida.setParcelasPagas(2);
        divida.setValorParcela(new BigDecimal("135.39"));
        divida.setDataContratacao(HOJE.minusMonths(4));
        divida.setDataProximoVencimento(HOJE.minusDays(40));
        divida.setDiasAtraso(40);
        divida.setStatus(StatusDivida.ATRASADA);
        return divida;
    }

    @Test
    void pagamentoTotalQuitaADivida() {
        Divida divida = dividaAtrasada();
        PagamentoService.aplicarPagamento(divida, new BigDecimal("1000.00"), HOJE);

        assertEquals(0, divida.getSaldoDevedor().signum());
        assertEquals(StatusDivida.QUITADA, divida.getStatus());
        assertEquals(12, divida.getParcelasPagas());
        assertEquals(0, divida.getDiasAtraso());
        assertNull(divida.getDataProximoVencimento());
    }

    @Test
    void pagamentoDeUmaParcelaIncrementaEZeraOAtraso() {
        Divida divida = dividaAtrasada();
        PagamentoService.aplicarPagamento(divida, new BigDecimal("135.39"), HOJE);

        assertEquals(new BigDecimal("864.61"), divida.getSaldoDevedor());
        assertEquals(3, divida.getParcelasPagas());
        assertEquals(0, divida.getDiasAtraso());
        assertEquals(StatusDivida.EM_DIA, divida.getStatus());
        // vencimento era 02/10; +1 mês = 02/11 (já passou) -> empurra para 02/12
        assertEquals(LocalDate.of(2026, 12, 2), divida.getDataProximoVencimento());
    }

    @Test
    void pagamentoDeDuasParcelasContaDuas() {
        Divida divida = dividaAtrasada();
        PagamentoService.aplicarPagamento(divida, new BigDecimal("300.00"), HOJE);
        assertEquals(4, divida.getParcelasPagas());
    }

    @Test
    void pagamentoMenorQueAParcelaSoAbateOSaldo() {
        Divida divida = dividaAtrasada();
        PagamentoService.aplicarPagamento(divida, new BigDecimal("50.00"), HOJE);

        assertEquals(new BigDecimal("950.00"), divida.getSaldoDevedor());
        assertEquals(2, divida.getParcelasPagas());
        assertEquals(40, divida.getDiasAtraso());
        assertEquals(StatusDivida.ATRASADA, divida.getStatus());
    }

    @Test
    void enquantoHouverSaldoSobraPeloMenosUmaParcela() {
        Divida divida = dividaAtrasada();
        divida.setParcelasPagas(11);
        PagamentoService.aplicarPagamento(divida, new BigDecimal("500.00"), HOJE);
        assertEquals(11, divida.getParcelasPagas());
        assertEquals(StatusDivida.EM_DIA, divida.getStatus());
    }

    // ---------------------------------------------------------------- cadastro de dívida

    @Test
    void completaCamposQueNaoVieram() {
        Divida divida = new Divida();
        divida.setValorOriginal(new BigDecimal("1000"));
        divida.setTaxaJurosMensal(new BigDecimal("2"));
        divida.setParcelasTotal(12);
        DividaService.completarCampos(divida, HOJE);

        assertEquals(new BigDecimal("1000"), divida.getSaldoDevedor());
        assertEquals(0, divida.getParcelasPagas());
        assertEquals(new BigDecimal("94.56"), divida.getValorParcela()); // Price
        assertEquals(0, divida.getDiasAtraso());
        assertEquals(StatusDivida.EM_DIA, divida.getStatus());
    }

    @Test
    void calculaAtrasoPeloVencimentoPassado() {
        Divida divida = dividaAtrasada();
        divida.setDiasAtraso(null);
        divida.setDataProximoVencimento(HOJE.minusDays(100));
        DividaService.completarCampos(divida, HOJE);

        assertEquals(100, divida.getDiasAtraso());
        assertEquals(StatusDivida.ATRASADA, divida.getStatus());
    }

    @Test
    void saldoZeradoViraQuitada() {
        Divida divida = dividaAtrasada();
        divida.setSaldoDevedor(new BigDecimal("0.00"));
        DividaService.completarCampos(divida, HOJE);

        assertEquals(StatusDivida.QUITADA, divida.getStatus());
        assertEquals(0, divida.getDiasAtraso());
    }

    @Test
    void parcelasPagasNaoPodemPassarDoTotal() {
        Divida divida = dividaAtrasada();
        divida.setParcelasPagas(13);
        assertThrows(RequisicaoInvalidaException.class, () -> DividaService.completarCampos(divida, HOJE));
    }
}
