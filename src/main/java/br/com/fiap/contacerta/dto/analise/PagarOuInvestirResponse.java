package br.com.fiap.contacerta.dto.analise;

import java.math.BigDecimal;
import java.util.List;

/** Saída de GET /usuarios/{id}/pagar-ou-investir. */
public record PagarOuInvestirResponse(
        BigDecimal selicAnual,                        // % ao ano, do Banco Central
        BigDecimal rendimentoMensalLiquidoPercentual, // quanto o Tesouro Selic rende por mês, já sem o IR
        BigDecimal aliquotaIrPercentual,              // 15% (referência de longo prazo)
        List<ComparacaoDivida> dividas,
        String comoCalculamos,
        boolean indicadoresDesatualizados,
        List<String> avisos) {

    /** A comparação de uma dívida com o investimento, com a mensagem pronta para exibir. */
    public record ComparacaoDivida(
            Long dividaId,
            String credor,
            String tipoDescricao,
            BigDecimal taxaDividaMensal,
            BigDecimal vezesMaisQueInvestir, // taxa da dívida / rendimento líquido do Tesouro
            String recomendacao,             // QUITAR ou INVESTIR
            String mensagem) {
    }
}
