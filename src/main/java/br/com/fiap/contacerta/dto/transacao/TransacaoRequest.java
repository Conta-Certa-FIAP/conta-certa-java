package br.com.fiap.contacerta.dto.transacao;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Entrada de POST /contas/{id}/transacoes (lançamento manual de entrada ou saída). */
public class TransacaoRequest {

    @NotNull(message = "A categoria é obrigatória")
    @Positive(message = "Categoria inválida")
    private Long categoriaId;

    /** Obrigatório apenas quando o meio é CARTAO_DEBITO ou CARTAO_CREDITO. */
    @Positive(message = "Cartão inválido")
    private Long cartaoId;

    @NotBlank(message = "A descrição é obrigatória")
    @Size(max = 120, message = "A descrição deve ter no máximo 120 caracteres")
    private String descricao;

    @NotNull(message = "O valor é obrigatório")
    @Positive(message = "O valor deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "O valor deve ter no máximo 10 dígitos e 2 casas decimais")
    private BigDecimal valor;

    @NotBlank(message = "O tipo é obrigatório")
    @Pattern(regexp = "ENTRADA|SAIDA", message = "O tipo deve ser ENTRADA ou SAIDA")
    private String tipo;

    @NotBlank(message = "O meio é obrigatório")
    @Pattern(regexp = "PIX|TRANSFERENCIA|CARTAO_DEBITO|CARTAO_CREDITO|BOLETO|OUTRO",
            message = "O meio deve ser PIX, TRANSFERENCIA, CARTAO_DEBITO, CARTAO_CREDITO, BOLETO ou OUTRO")
    private String meio;

    /** Opcional: se não vier, a API usa a data de hoje. Formato AAAA-MM-DD. */
    @PastOrPresent(message = "A data não pode ser no futuro")
    private LocalDate data;

    /** Opcional (padrão false): a pessoa marcou "repetir todo mês". */
    private boolean recorrente;

    public boolean isRecorrente() { return recorrente; }
    public void setRecorrente(boolean recorrente) { this.recorrente = recorrente; }

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }

    public Long getCartaoId() { return cartaoId; }
    public void setCartaoId(Long cartaoId) { this.cartaoId = cartaoId; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getMeio() { return meio; }
    public void setMeio(String meio) { this.meio = meio; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
}
