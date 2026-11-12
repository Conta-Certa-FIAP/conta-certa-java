package br.com.fiap.contacerta.client;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Um ponto de uma série do Banco Central: o valor em uma data. */
public record DadoSerie(LocalDate data, BigDecimal valor) {
}
