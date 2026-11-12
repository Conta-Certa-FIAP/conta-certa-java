package br.com.fiap.contacerta.dto.analise;

import java.math.BigDecimal;
import java.util.List;

/**
 * Saída de GET /usuarios/{id}/situacao: situação financeira + nota de saúde (0 a 100).
 * Campos que dependem da renda vêm nulos quando a renda é desconhecida (ver "avisos").
 */
public record SituacaoResponse(
        String situacao,                      // CONFORTAVEL, ATENCAO, EM_RISCO ou INADIMPLENTE
        String titulo,                        // texto curto, ex.: "Em risco"
        String descricao,                     // explicação e orientação
        int nota,                             // 0 a 100
        BigDecimal rendaMedia,
        String origemRenda,                   // EXTRATO (média das entradas) ou DECLARADA (cadastro)
        BigDecimal gastosEssenciaisMedia,
        BigDecimal totalParcelasMes,          // soma das parcelas das dívidas não quitadas
        BigDecimal comprometimentoPercentual, // totalParcelasMes / rendaMedia, em %
        BigDecimal totalDividas,              // soma dos saldos devedores
        int dividasAtivas,
        int dividasAtrasadas,
        BigDecimal saldoContas,
        BigDecimal totalCofres,
        List<ItemNota> detalhesNota,          // o que somou/tirou pontos
        String comoCalculamos,
        List<String> avisos) {
}
