package br.com.fiap.contacerta.dto.indicador;

import java.math.BigDecimal;
import java.util.List;

/** Saída de GET /indicadores/historico?meses=12: dados para os gráficos da "situação do país". */
public record HistoricoResponse(
        int meses,
        List<SerieHistorico> indicadores,
        boolean desatualizado,
        String fonte) {

    /** O histórico de um indicador, um ponto por mês (o último valor de cada mês). */
    public record SerieHistorico(String codigo, int serieBcb, String nome, String unidade,
                                 List<PontoHistorico> pontos) {
    }

    /**
     * Um mês do histórico.
     * "variacao" compara com o mês anterior: "subiu", "caiu" ou "estável" (nulo no primeiro ponto,
     * quando não há mês anterior para comparar).
     */
    public record PontoHistorico(String mes, BigDecimal valor, BigDecimal diferenca, String variacao) {
    }
}
