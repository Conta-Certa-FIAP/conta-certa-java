package br.com.fiap.contacerta.service.front;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.fiap.contacerta.model.Divida;

/** Regras de cálculo do DividaFrontService que não dependem do banco. */
class DividaFrontServiceTest {

    @Test
    void minimoInformadoEhRespeitado() {
        assertEquals(new BigDecimal("150.00"),
                DividaFrontService.parcela(new BigDecimal("150"), new BigDecimal("1000.00"), new BigDecimal("15.000"), 1, 0));
    }

    @Test
    void semParcelasEMinimoZeroUsaQuinzePorCentoDoSaldo() {
        // R$ 1.000 a 5% a.m.: 15% (R$ 150) é mais que os juros (R$ 50)
        assertEquals(new BigDecimal("150.00"),
                DividaFrontService.parcela(BigDecimal.ZERO, new BigDecimal("1000.00"), new BigDecimal("5.000"), 1, 0));
    }

    @Test
    void semParcelasOMinimoNuncaFicaAbaixoDosJuros() {
        // R$ 1.000 a 20% a.m.: os juros (R$ 200) passam dos 15%
        assertEquals(new BigDecimal("200.00"),
                DividaFrontService.parcela(null, new BigDecimal("1000.00"), new BigDecimal("20.000"), 1, 0));
    }

    @Test
    void parceladaSemMinimoUsaTabelaPriceSobreAsParcelasQueFaltam() {
        // R$ 1.200 sem juros e 4 parcelas restantes = R$ 300
        assertEquals(new BigDecimal("300.00"),
                DividaFrontService.parcela(BigDecimal.ZERO, new BigDecimal("1200.00"), BigDecimal.ZERO, 12, 8));
    }

    @Test
    void valorOriginalEstimadoPelasParcelasPagas() {
        Divida divida = new Divida();
        divida.setSaldoDevedor(new BigDecimal("900.00"));
        divida.setParcelasTotal(12);
        divida.setParcelasPagas(3);
        // 900 / 9 restantes * 12 = 1.200 -> progresso de 25%, igual a 3 de 12
        assertEquals(new BigDecimal("1200.00"), DividaFrontService.valorOriginalEstimado(divida));

        divida.setParcelasPagas(0);
        assertEquals(new BigDecimal("900.00"), DividaFrontService.valorOriginalEstimado(divida));
    }
}
