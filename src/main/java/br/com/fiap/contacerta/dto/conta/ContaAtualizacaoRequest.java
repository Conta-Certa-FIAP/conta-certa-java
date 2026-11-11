package br.com.fiap.contacerta.dto.conta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Entrada de PUT /contas/{id}: corrige agência e número. Banco e saldo não mudam por aqui. */
public class ContaAtualizacaoRequest {

    @NotBlank(message = "A agência é obrigatória")
    @Pattern(regexp = "^\\d{4}$", message = "A agência deve ter exatamente 4 números")
    private String agencia;

    @NotBlank(message = "O número da conta é obrigatório")
    @Pattern(regexp = "^\\d{1,10}-?[\\dXx]$", message = "Informe a conta com o dígito, ex.: 12345-6")
    private String numeroConta;

    public String getAgencia() { return agencia; }
    public void setAgencia(String agencia) { this.agencia = agencia; }

    public String getNumeroConta() { return numeroConta; }
    public void setNumeroConta(String numeroConta) { this.numeroConta = numeroConta; }
}
