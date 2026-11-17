package br.com.fiap.contacerta.dto.front;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma transação no formato do front (item de GET /transactions e resposta do POST).
 *
 * @param type      ENTRADA ou SAIDA
 * @param category  MERCADO, TRANSPORTE, MORADIA, CONTAS, LAZER, TRABALHO ou OUTROS
 * @param amount    sempre positivo; o sinal vem de "type"
 * @param recurring "repetir todo mês"
 */
public record TransactionDto(
        Long id,
        String type,
        String description,
        String category,
        BigDecimal amount,
        LocalDate date,
        boolean recurring) {
}
