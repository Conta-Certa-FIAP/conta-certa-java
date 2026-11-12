package br.com.fiap.contacerta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.fiap.contacerta.model.Cofre;
import br.com.fiap.contacerta.service.CofreService.ProgressoMeta;

/** Progresso das metas do cofre (sem banco de dados). */
class CofreServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 11);

    private static Cofre cofre(String meta, String guardado, LocalDate limite) {
        Cofre cofre = new Cofre();
        cofre.setValorMeta(meta == null ? null : new BigDecimal(meta));
        cofre.setValorGuardado(new BigDecimal(guardado));
        cofre.setDataLimite(limite);
        return cofre;
    }

    @Test
    void semMetaNaoTemProgresso() {
        ProgressoMeta progresso = CofreService.calcularProgresso(cofre(null, "500.00", null), HOJE);
        assertNull(progresso.percentualConcluido());
        assertNull(progresso.valorFaltante());
        assertFalse(progresso.metaAtingida());
    }

    @Test
    void metaSemPrazoMostraPercentualEQuantoFalta() {
        ProgressoMeta progresso = CofreService.calcularProgresso(cofre("2000.00", "500.00", null), HOJE);
        assertEquals(new BigDecimal("25.00"), progresso.percentualConcluido());
        assertEquals(new BigDecimal("1500.00"), progresso.valorFaltante());
        assertNull(progresso.valorMensalNecessario());
    }

    @Test
    void metaComPrazoCalculaQuantoGuardarPorMes() {
        // faltam 1500 e o prazo é daqui a exatamente 6 meses -> 250 por mês
        ProgressoMeta progresso = CofreService.calcularProgresso(cofre("2000.00", "500.00", HOJE.plusMonths(6)), HOJE);
        assertEquals(6, progresso.mesesRestantes());
        assertEquals(new BigDecimal("250.00"), progresso.valorMensalNecessario());
    }

    @Test
    void pedacoDeMesContaComoMesInteiroECentavosSobemParaCima() {
        // 2 meses e 10 dias -> 3 meses; 1000 / 3 = 333,333... -> 333,34
        ProgressoMeta progresso = CofreService.calcularProgresso(
                cofre("1000.00", "0.00", HOJE.plusMonths(2).plusDays(10)), HOJE);
        assertEquals(3, progresso.mesesRestantes());
        assertEquals(new BigDecimal("333.34"), progresso.valorMensalNecessario());
    }

    @Test
    void metaAtingidaFicaEm100PorCento() {
        ProgressoMeta progresso = CofreService.calcularProgresso(cofre("1000.00", "1200.00", HOJE.plusMonths(3)), HOJE);
        assertEquals(new BigDecimal("100.00"), progresso.percentualConcluido());
        assertEquals(0, progresso.valorFaltante().signum());
        assertTrue(progresso.metaAtingida());
        assertNull(progresso.valorMensalNecessario());
    }

    @Test
    void prazoVencidoSemBaterAMeta() {
        ProgressoMeta progresso = CofreService.calcularProgresso(cofre("1000.00", "400.00", HOJE.minusDays(1)), HOJE);
        assertTrue(progresso.prazoVencido());
        assertNull(progresso.valorMensalNecessario());
        assertEquals(new BigDecimal("600.00"), progresso.valorFaltante());
    }
}
