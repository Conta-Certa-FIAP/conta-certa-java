package br.com.fiap.contacerta.dto.analise;

import java.math.BigDecimal;
import java.util.List;

/** Saída de GET /usuarios/{id}/resumo-mensal?mes=AAAA-MM. */
public record ResumoMensalResponse(
        String mes,                               // AAAA-MM
        BigDecimal totalEntradas,
        BigDecimal totalSaidas,
        BigDecimal saldoDoMes,                    // entradas - saídas
        BigDecimal rendaConsiderada,              // base do "% da renda" (nula se desconhecida)
        List<GastoCategoria> gastosPorCategoria,  // saídas por categoria, da maior para a menor
        List<GastoCategoria> top3NaoEssenciais,
        String fraseDeImpacto) {                  // nula quando não há o que dizer

    /** Total gasto em uma categoria e quanto isso representa da renda. */
    public record GastoCategoria(Long categoriaId, String categoria, boolean essencial,
                                 BigDecimal total, BigDecimal percentualDaRenda) {
    }
}
