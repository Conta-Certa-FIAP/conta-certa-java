package br.com.fiap.contacerta.dto.indicador;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Saída de GET /indicadores: a "situação do país" com dados reais do Banco Central. */
public record IndicadoresResponse(
        Indicador selic,           // meta da Selic, % ao ano
        Indicador ipcaMensal,      // inflação do mês, %
        Indicador ipca12Meses,     // inflação acumulada em 12 meses, %
        Indicador dolar,           // dólar comercial (venda), R$
        BigDecimal juroReal,       // (1 + Selic) / (1 + IPCA 12 meses) - 1, em %
        String juroRealExplicacao,
        LocalDateTime atualizadoEm,
        boolean desatualizado,     // true = Banco Central fora do ar; estes são os últimos dados salvos
        String fonte) {

    /** Um indicador: o último valor conhecido e a data a que ele se refere. */
    public record Indicador(String codigo, int serieBcb, String nome, String unidade,
                            BigDecimal valor, LocalDate dataReferencia) {
    }
}
