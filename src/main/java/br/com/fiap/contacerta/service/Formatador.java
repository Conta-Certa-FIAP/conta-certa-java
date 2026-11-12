package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formata números do jeito brasileiro para as mensagens da API (ex.: "R$ 1.234,56" e "13,5%"). */
public final class Formatador {

    private static final DecimalFormatSymbols SIMBOLOS = new DecimalFormatSymbols(Locale.forLanguageTag("pt-BR"));

    private Formatador() {
    }

    /** 1234.5 -> "R$ 1.234,50" */
    public static String reais(BigDecimal valor) {
        // DecimalFormat não é thread-safe: criamos um novo a cada chamada (é barato).
        return "R$ " + new DecimalFormat("#,##0.00", SIMBOLOS).format(valor.setScale(2, RoundingMode.HALF_UP));
    }

    /** 13.50 -> "13,5%"  |  2.00 -> "2%" */
    public static String percentual(BigDecimal valor) {
        return new DecimalFormat("#,##0.##", SIMBOLOS).format(valor) + "%";
    }

    /** 13.0 -> "13"  |  14.1 -> "14,1" (número com no máximo 1 casa decimal) */
    public static String numero(BigDecimal valor) {
        return new DecimalFormat("#,##0.#", SIMBOLOS).format(valor);
    }

    /** 7.5 horas -> "7h30" */
    public static String horas(double horas) {
        long minutosTotais = Math.round(horas * 60);
        return (minutosTotais / 60) + "h" + String.format("%02d", minutosTotais % 60);
    }
}
