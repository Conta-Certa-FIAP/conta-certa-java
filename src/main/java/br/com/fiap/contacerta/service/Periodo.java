package br.com.fiap.contacerta.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;

/**
 * Um intervalo de datas: de "inicio" (inclusive) até "fimExclusivo" (o dia seguinte ao último).
 * Usar o fim exclusivo deixa o SQL simples: dt >= inicio AND dt < fimExclusivo.
 */
public record Periodo(LocalDate inicio, LocalDate fimExclusivo) {

    /** Mês de calendário a partir do texto "AAAA-MM" (ex.: 2026-11). Formato errado -> 400. */
    public static Periodo doMes(String mes) {
        try {
            return doMes(YearMonth.parse(mes.trim()));
        } catch (DateTimeParseException e) {
            throw new RequisicaoInvalidaException("O parâmetro 'mes' deve estar no formato AAAA-MM, ex.: 2026-11.");
        }
    }

    public static Periodo doMes(YearMonth mes) {
        return new Periodo(mes.atDay(1), mes.plusMonths(1).atDay(1));
    }

    /**
     * Janela "móvel" dos últimos N meses terminando hoje.
     * Ex.: hoje = 11/11 e 3 meses -> de 12/08 até 11/11 (inclusive).
     * Preferimos a janela móvel ao mês de calendário para a análise não "zerar" no dia 1º.
     */
    public static Periodo ultimosMeses(LocalDate hoje, int meses) {
        return new Periodo(hoje.minusMonths(meses).plusDays(1), hoje.plusDays(1));
    }
}
