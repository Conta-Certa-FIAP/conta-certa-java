package br.com.fiap.contacerta.dto.front;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Entrada de POST /debts e PUT /debts/{id}: o que a pessoa preenche no formulário do front.
 * "marketRate" e "status" não vêm: quem calcula é a API.
 * O front já valida estes campos, mas a API confere de novo (nunca se confia só na tela).
 */
public record DebtInputDto(
        @NotBlank(message = "O nome da dívida é obrigatório")
        @Size(min = 2, max = 60, message = "O nome deve ter entre 2 e 60 caracteres")
        String name,

        @NotBlank(message = "O tipo de crédito é obrigatório")
        String type,

        @NotBlank(message = "O credor é obrigatório")
        @Size(min = 2, max = 60, message = "O credor deve ter entre 2 e 60 caracteres")
        String creditor,

        @NotNull(message = "O valor devido é obrigatório")
        @Positive(message = "O valor devido deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "O valor devido deve ter no máximo 10 dígitos e 2 casas decimais")
        BigDecimal balance,

        @NotNull(message = "A taxa de juros é obrigatória (use 0 se não houver juros)")
        @DecimalMin(value = "0", message = "A taxa de juros não pode ser negativa")
        @DecimalMax(value = "100", message = "A taxa de juros deve ser de no máximo 100% ao mês")
        BigDecimal monthlyRate,

        @PositiveOrZero(message = "O pagamento mínimo não pode ser negativo")
        @Digits(integer = 10, fraction = 2, message = "O pagamento mínimo deve ter no máximo 10 dígitos e 2 casas decimais")
        BigDecimal minimumPayment,

        @NotNull(message = "O próximo vencimento é obrigatório")
        LocalDate nextDueDate,

        @Min(value = 0, message = "As parcelas pagas não podem ser negativas")
        Integer installmentsPaid,

        @Min(value = 1, message = "O total de parcelas deve ser no mínimo 1")
        @Max(value = 9999, message = "O total de parcelas deve ser no máximo 9999")
        Integer installmentsTotal) {
}
