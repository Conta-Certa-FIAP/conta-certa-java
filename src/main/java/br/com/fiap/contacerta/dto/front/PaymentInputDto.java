package br.com.fiap.contacerta.dto.front;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

/**
 * Entrada de POST /debts/{id}/payments ("Registrar pagamento" na tela da dívida).
 *
 * @param accountId opcional: conta de onde sai o dinheiro (abate o saldo e aparece no extrato)
 * @param date      opcional: se não vier, usa a data de hoje
 */
public record PaymentInputDto(
        @NotNull(message = "O valor do pagamento é obrigatório")
        @Positive(message = "O valor do pagamento deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "O valor deve ter no máximo 10 dígitos e 2 casas decimais")
        BigDecimal amount,

        @Positive(message = "Conta inválida")
        Long accountId,

        @PastOrPresent(message = "A data do pagamento não pode ser no futuro")
        LocalDate date) {
}
