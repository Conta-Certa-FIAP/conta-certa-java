package br.com.fiap.contacerta.dto.investimento;

import java.math.BigDecimal;
import java.util.List;

/** Saída de POST /investimentos/tesouro-selic/simular. */
public record SimulacaoTesouroResponse(
        BigDecimal valorInicial,
        BigDecimal aporteMensal,
        int meses,
        BigDecimal selicAnual,                     // % ao ano usada na simulação
        BigDecimal ipcaAnual,                      // % ao ano usada na simulação
        BigDecimal taxaMensalPercentual,           // (1 + Selic)^(1/12) - 1, em %
        BigDecimal totalInvestido,
        BigDecimal valorBrutoFinal,
        BigDecimal rendimentoBruto,
        BigDecimal impostoDeRenda,
        BigDecimal valorLiquidoFinal,
        BigDecimal rendimentoLiquido,
        BigDecimal rentabilidadeLiquidaPercentual, // rendimento líquido / total investido
        BigDecimal ipcaNoPeriodoPercentual,
        BigDecimal juroRealPercentual,             // rentabilidade descontada a inflação do período
        BigDecimal valorFinalEmDinheiroDeHoje,     // valor líquido trazido a preços de hoje
        List<MesSimulacao> evolucao,               // mês a mês, para o gráfico
        String origemDasTaxas,
        List<String> simplificacoes) {

    /** Situação do investimento ao fim de um mês (como se o resgate fosse feito naquele mês). */
    public record MesSimulacao(int mes, BigDecimal totalInvestido, BigDecimal valorBruto,
                               BigDecimal impostoDeRenda, BigDecimal valorLiquido) {
    }
}
