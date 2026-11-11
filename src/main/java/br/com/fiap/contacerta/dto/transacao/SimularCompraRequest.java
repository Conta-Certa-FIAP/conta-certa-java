package br.com.fiap.contacerta.dto.transacao;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Entrada de POST /contas/{id}/transacoes/simular-compra (botão "simular compra" do front).
 * Só o tipo do cartão é obrigatório; valor e categoria, se não vierem, são sorteados.
 */
public class SimularCompraRequest {

    @NotBlank(message = "Informe o tipo do cartão")
    @Pattern(regexp = "CREDITO|DEBITO", message = "O tipo do cartão deve ser CREDITO ou DEBITO")
    private String tipoCartao;

    @Positive(message = "O valor deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "O valor deve ter no máximo 10 dígitos e 2 casas decimais")
    private BigDecimal valor;

    @Positive(message = "Categoria inválida")
    private Long categoriaId;

    public String getTipoCartao() { return tipoCartao; }
    public void setTipoCartao(String tipoCartao) { this.tipoCartao = tipoCartao; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
}
