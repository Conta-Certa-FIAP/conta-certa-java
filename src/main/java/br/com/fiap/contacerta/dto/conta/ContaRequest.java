package br.com.fiap.contacerta.dto.conta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Entrada de POST /usuarios/{id}/contas ("conectar um banco"). */
public class ContaRequest {

    @NotNull(message = "Escolha o banco")
    @Positive(message = "Banco inválido")
    private Long bancoId;

    @NotBlank(message = "A agência é obrigatória")
    @Pattern(regexp = "^\\d{4}$", message = "A agência deve ter exatamente 4 números")
    private String agencia;

    // Até 10 números + dígito verificador (número ou X), com ou sem hífen. Ex.: 12345-6
    @NotBlank(message = "O número da conta é obrigatório")
    @Pattern(regexp = "^\\d{1,10}-?[\\dXx]$", message = "Informe a conta com o dígito, ex.: 12345-6")
    private String numeroConta;

    /** true = a API gera cartões, 3 meses de extrato e dívidas, como se viesse do Open Finance. */
    private boolean importarDadosSimulados;

    public Long getBancoId() { return bancoId; }
    public void setBancoId(Long bancoId) { this.bancoId = bancoId; }

    public String getAgencia() { return agencia; }
    public void setAgencia(String agencia) { this.agencia = agencia; }

    public String getNumeroConta() { return numeroConta; }
    public void setNumeroConta(String numeroConta) { this.numeroConta = numeroConta; }

    public boolean isImportarDadosSimulados() { return importarDadosSimulados; }
    public void setImportarDadosSimulados(boolean importarDadosSimulados) { this.importarDadosSimulados = importarDadosSimulados; }
}
