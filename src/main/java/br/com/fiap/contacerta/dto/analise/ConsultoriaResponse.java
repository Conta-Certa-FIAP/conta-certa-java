package br.com.fiap.contacerta.dto.analise;

import java.math.BigDecimal;
import java.util.List;

/** Saída de GET /usuarios/{id}/consultoria: quanto pagar por mês e como sair das dívidas. */
public record ConsultoriaResponse(
        BigDecimal rendaMedia,
        BigDecimal gastosEssenciaisMedia,
        BigDecimal folga,                     // rendaMedia - gastosEssenciaisMedia
        BigDecimal limiteDaRenda,             // 40% da rendaMedia
        BigDecimal valorSugeridoParaDividas,  // o menor entre a folga e o limite
        BigDecimal totalParcelasMes,          // quanto as parcelas atuais somam
        PlanoQuitacao avalanche,              // nulo se não houver dívida ou valor para pagar
        PlanoQuitacao bolaDeNeve,
        ComparacaoPlanos comparacao,
        List<SugestaoCorte> sugestoesDeCorte,
        String comoCalculamos,
        List<String> avisos) {

    /** Diferença entre os dois métodos (economia positiva = avalanche sai mais barato). */
    public record ComparacaoPlanos(BigDecimal economiaAvalanche, int mesesDeDiferenca, String mensagem) {
    }

    /** Categoria não essencial onde dá para cortar e quanto se economiza cortando metade. */
    public record SugestaoCorte(String categoria, BigDecimal gastoUltimoMes, BigDecimal economiaCortandoMetade) {
    }
}
