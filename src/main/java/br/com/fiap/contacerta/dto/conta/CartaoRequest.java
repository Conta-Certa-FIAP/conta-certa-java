package br.com.fiap.contacerta.dto.conta;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/** Entrada de POST /contas/{id}/cartoes. Cartão de CREDITO exige limite (conferido no service). */
public class CartaoRequest {

    @NotBlank(message = "O tipo é obrigatório")
    @Pattern(regexp = "CREDITO|DEBITO", message = "O tipo deve ser CREDITO ou DEBITO")
    private String tipo;

    @NotBlank(message = "Informe os 4 últimos números do cartão")
    @Pattern(regexp = "^\\d{4}$", message = "O final do cartão deve ter exatamente 4 números")
    private String numeroFinal;

    @PositiveOrZero(message = "O limite não pode ser negativo")
    @Digits(integer = 10, fraction = 2, message = "O limite deve ter no máximo 10 dígitos e 2 casas decimais")
    private BigDecimal limite;

    @Min(value = 1, message = "O dia de vencimento deve ser entre 1 e 28")
    @Max(value = 28, message = "O dia de vencimento deve ser entre 1 e 28")
    private Integer diaVencimento;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getNumeroFinal() { return numeroFinal; }
    public void setNumeroFinal(String numeroFinal) { this.numeroFinal = numeroFinal; }

    public BigDecimal getLimite() { return limite; }
    public void setLimite(BigDecimal limite) { this.limite = limite; }

    public Integer getDiaVencimento() { return diaVencimento; }
    public void setDiaVencimento(Integer diaVencimento) { this.diaVencimento = diaVencimento; }
}
