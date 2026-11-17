package br.com.fiap.contacerta.dto.front;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Entrada de PUT /plan: quanto a pessoa pode pagar por mês e o método escolhido. */
public record PlanUpdateDto(
        @NotNull(message = "Informe quanto você pode pagar por mês")
        @Positive(message = "O valor mensal deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "O valor mensal deve ter no máximo 10 dígitos e 2 casas decimais")
        BigDecimal monthlyBudget,

        @NotBlank(message = "Escolha o método")
        @Pattern(regexp = "AVALANCHE|BOLA_DE_NEVE", message = "O método deve ser AVALANCHE ou BOLA_DE_NEVE")
        String method) {
}
