package br.com.fiap.contacerta.service.front;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;

/** Lê o parâmetro "month" (AAAA-MM) das rotas do front. */
public final class MesFront {

    static final int ANO_MINIMO = 2000;
    static final int ANO_MAXIMO = 2100;

    private MesFront() {
    }

    /** Sem o parâmetro, vale o mês atual. Formato errado vira 400. */
    public static YearMonth ler(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        YearMonth mes;
        try {
            mes = YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw new RequisicaoInvalidaException("O parâmetro 'month' deve estar no formato AAAA-MM, ex.: 2026-11.");
        }
        // Anos absurdos (ex.: 9999) estourariam as datas do Oracle e virariam erro 500.
        if (mes.getYear() < ANO_MINIMO || mes.getYear() > ANO_MAXIMO) {
            throw new RequisicaoInvalidaException("O parâmetro 'month' deve ser um mês entre "
                    + ANO_MINIMO + " e " + ANO_MAXIMO + ".");
        }
        return mes;
    }
}
