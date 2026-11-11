package br.com.fiap.contacerta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/** Testes das fórmulas financeiras (Price e companhia). Não dependem do banco. */
class CalculadoraFinanceiraTest {

    @Test
    void parcelaPriceComJuros() {
        // R$ 1.000 em 12x a 2% ao mês -> 1000 * 0,02 / (1 - 1,02^-12) = 94,56
        BigDecimal parcela = CalculadoraFinanceira.parcelaPrice(new BigDecimal("1000"), new BigDecimal("2"), 12);
        assertEquals(new BigDecimal("94.56"), parcela);
    }

    @Test
    void parcelaPriceSemJurosEhDivisaoSimples() {
        BigDecimal parcela = CalculadoraFinanceira.parcelaPrice(new BigDecimal("1200"), BigDecimal.ZERO, 12);
        assertEquals(new BigDecimal("100.00"), parcela);
    }

    @Test
    void parcelaPriceEmUmaParcelaEhValorMaisJuros() {
        BigDecimal parcela = CalculadoraFinanceira.parcelaPrice(new BigDecimal("1000"), new BigDecimal("10"), 1);
        assertEquals(new BigDecimal("1100.00"), parcela);
    }

    @Test
    void valorPresenteEhOInversoDaPrice() {
        BigDecimal valor = CalculadoraFinanceira.valorPresente(new BigDecimal("94.56"), new BigDecimal("2"), 12);
        // volta para (quase) R$ 1.000: a diferença é só o arredondamento da parcela
        assertEquals(1000.0, valor.doubleValue(), 0.05);
    }

    @Test
    void saldoAposParcelas() {
        BigDecimal valor = new BigDecimal("1000");
        BigDecimal taxa = new BigDecimal("2");
        assertEquals(new BigDecimal("1000.00"), CalculadoraFinanceira.saldoAposParcelas(valor, taxa, 12, 0));
        assertEquals(new BigDecimal("0.00"), CalculadoraFinanceira.saldoAposParcelas(valor, taxa, 12, 12));
        // depois da 1ª parcela: 1000 * 1,02 - 94,56 = 925,44
        assertEquals(925.44, CalculadoraFinanceira.saldoAposParcelas(valor, taxa, 12, 1).doubleValue(), 0.01);
    }

    @Test
    void jurosDoMes() {
        assertEquals(new BigDecimal("130.00"),
                CalculadoraFinanceira.jurosDoMes(new BigDecimal("1000"), new BigDecimal("13")));
    }

    @Test
    void percentual() {
        assertEquals(new BigDecimal("25.00"),
                CalculadoraFinanceira.percentual(new BigDecimal("500"), new BigDecimal("2000")));
        assertNull(CalculadoraFinanceira.percentual(new BigDecimal("500"), BigDecimal.ZERO));
    }

    @Test
    void taxaMensalEquivalente() {
        // 12,6825% ao ano equivale a 1% ao mês em juros compostos (1,01^12 = 1,126825)
        assertEquals(0.01, CalculadoraFinanceira.taxaMensalEquivalente(12.6825), 0.000001);
    }
}
