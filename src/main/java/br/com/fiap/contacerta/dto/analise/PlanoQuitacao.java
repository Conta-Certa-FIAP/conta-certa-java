package br.com.fiap.contacerta.dto.analise;

import java.math.BigDecimal;
import java.util.List;

/** Resultado da simulação de quitação por um método (AVALANCHE ou BOLA_DE_NEVE). */
public record PlanoQuitacao(
        String metodo,
        String descricao,
        boolean quitaTudo,                    // false = em 360 meses ainda sobra dívida
        int meses,                            // meses até quitar (ou 360 se não quitar)
        BigDecimal totalPago,
        BigDecimal totalJuros,
        List<QuitacaoDivida> ordemQuitacao,   // qual dívida acaba em qual mês
        List<MesPlano> evolucao,              // mês a mês, para o gráfico
        String aviso) {

    /** Uma dívida quitada no plano e o mês em que isso acontece. */
    public record QuitacaoDivida(Long dividaId, String credor, int mes) {
    }

    /** Um mês do plano: juros cobrados, valor pago e quanto ainda falta no total. */
    public record MesPlano(int mes, BigDecimal juros, BigDecimal pago, BigDecimal saldoDevedorTotal) {
    }
}
