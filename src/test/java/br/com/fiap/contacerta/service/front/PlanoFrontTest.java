package br.com.fiap.contacerta.service.front;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.fiap.contacerta.dto.front.PlanDto;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.TipoDivida;
import br.com.fiap.contacerta.service.PlanoQuitacaoCalculadora.Metodo;

class PlanoFrontTest {

    private static final YearMonth NOVEMBRO = YearMonth.of(2026, 11);

    private static Divida divida(long id, TipoDivida tipo, String saldo, String taxa, String parcela) {
        Divida divida = DividaFrontTest.divida(tipo, taxa, 0);
        divida.setId(id);
        divida.setValorOriginal(new BigDecimal(saldo));
        divida.setSaldoDevedor(new BigDecimal(saldo));
        divida.setValorParcela(new BigDecimal(parcela));
        return divida;
    }

    @Test
    void semDividasOPlanoVemVazio() {
        PlanDto plano = PlanoFront.montar(List.of(), new BigDecimal("300.00"), Metodo.AVALANCHE, NOVEMBRO);
        assertTrue(plano.order().isEmpty());
        assertTrue(plano.options().isEmpty());
        assertNull(plano.debtFreeDate());
    }

    @Test
    void avalanchePagaPrimeiroAMaiorTaxaEBolaDeNeveOMenorSaldo() {
        List<Divida> dividas = List.of(
                divida(1, TipoDivida.CARTAO_ROTATIVO, "2000.00", "14.000", "100.00"),
                divida(2, TipoDivida.CREDIARIO, "300.00", "4.000", "50.00"));

        PlanDto avalanche = PlanoFront.montar(dividas, new BigDecimal("800.00"), Metodo.AVALANCHE, NOVEMBRO);
        PlanDto bolaDeNeve = PlanoFront.montar(dividas, new BigDecimal("800.00"), Metodo.BOLA_DE_NEVE, NOVEMBRO);

        assertEquals(1L, avalanche.order().get(0).debtId());
        assertEquals(2L, bolaDeNeve.order().get(0).debtId());
        assertNotNull(avalanche.debtFreeDate());
        assertEquals(2, avalanche.options().size());
        // Com uma dívida de 14% a.m. o avalanche economiza juros, então é o recomendado.
        assertTrue(avalanche.options().stream().anyMatch(o -> o.method().equals("AVALANCHE") && o.recommended()));
        assertNull(avalanche.warning());
    }

    @Test
    void valorQueNaoCobreOsJurosGeraAvisoESemDataDeQuitacao() {
        List<Divida> dividas = List.of(divida(1, TipoDivida.CARTAO_ROTATIVO, "5000.00", "15.000", "200.00"));

        PlanDto plano = PlanoFront.montar(dividas, new BigDecimal("100.00"), Metodo.AVALANCHE, NOVEMBRO);

        assertNull(plano.debtFreeDate());
        assertNull(plano.order().get(0).payoffDate());
        assertNotNull(plano.warning());
    }
}
