package br.com.fiap.contacerta.dto.analise;

import java.math.BigDecimal;

/** Saída de GET /usuarios/{id}/custo-em-horas?valor=: quanto tempo de trabalho custa uma compra. */
public record CustoEmHorasResponse(
        BigDecimal valor,
        BigDecimal rendaMedia,
        int horasTrabalhoMes,
        BigDecimal valorHora,      // rendaMedia / horasTrabalhoMes
        BigDecimal horas,          // valor / valorHora (ex.: 7.50)
        String horasFormatadas,    // ex.: "7h30"
        String mensagem) {         // ex.: "Essa compra custa 7h30 do seu trabalho"
}
