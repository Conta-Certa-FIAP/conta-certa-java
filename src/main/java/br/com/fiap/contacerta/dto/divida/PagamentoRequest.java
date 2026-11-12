package br.com.fiap.contacerta.dto.divida;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

/** Entrada de POST /dividas/{id}/pagamentos. */
public class PagamentoRequest {

    @NotNull(message = "O valor do pagamento é obrigatório")
    @Positive(message = "O valor do pagamento deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "O valor deve ter no máximo 10 dígitos e 2 casas decimais")
    private BigDecimal valor;

    /** Opcional: conta de onde sai o dinheiro. Se vier, o saldo é abatido e nasce uma transação de saída. */
    @Positive(message = "Conta inválida")
    private Long contaId;

    /** Opcional: se não vier, usa a data de hoje. */
    @PastOrPresent(message = "A data do pagamento não pode ser no futuro")
    private LocalDate data;

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public Long getContaId() { return contaId; }
    public void setContaId(Long contaId) { this.contaId = contaId; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
}
