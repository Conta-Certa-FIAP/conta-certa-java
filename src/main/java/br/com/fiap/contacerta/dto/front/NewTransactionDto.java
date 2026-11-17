package br.com.fiap.contacerta.dto.front;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Entrada de POST /transactions.
 *
 * @param recurring opcional (nulo = false)
 * @param accountId opcional: em qual conta lançar. Se não vier, a API usa a primeira conta da pessoa.
 */
public record NewTransactionDto(
        @NotBlank(message = "O tipo é obrigatório")
        @Pattern(regexp = "ENTRADA|SAIDA", message = "O tipo deve ser ENTRADA ou SAIDA")
        String type,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(min = 2, max = 80, message = "A descrição deve ter entre 2 e 80 caracteres")
        String description,

        @NotBlank(message = "A categoria é obrigatória")
        @Pattern(regexp = "MERCADO|TRANSPORTE|MORADIA|CONTAS|LAZER|TRABALHO|OUTROS", message = "Categoria inválida")
        String category,

        @NotNull(message = "O valor é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "O valor deve ter no máximo 10 dígitos e 2 casas decimais")
        BigDecimal amount,

        @NotNull(message = "A data é obrigatória")
        @PastOrPresent(message = "A data não pode ser no futuro")
        LocalDate date,

        Boolean recurring,

        @Positive(message = "Conta inválida")
        Long accountId) {
}
