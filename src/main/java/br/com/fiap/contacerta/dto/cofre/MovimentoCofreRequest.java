package br.com.fiap.contacerta.dto.cofre;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Entrada de POST /cofres/{id}/depositos e POST /cofres/{id}/resgates. */
public class MovimentoCofreRequest {

    /** Conta de onde sai o dinheiro (depósito) ou para onde ele volta (resgate). */
    @NotNull(message = "A conta é obrigatória")
    @Positive(message = "Conta inválida")
    private Long contaId;

    @NotNull(message = "O valor é obrigatório")
    @Positive(message = "O valor deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "O valor deve ter no máximo 10 dígitos e 2 casas decimais")
    private BigDecimal valor;

    public Long getContaId() { return contaId; }
    public void setContaId(Long contaId) { this.contaId = contaId; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
}
