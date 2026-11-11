package br.com.fiap.contacerta.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Espelha a tabela tb_conta_bancaria: a conta do usuário em um banco.
 * É a tabela associativa do relacionamento N:N entre usuário e banco.
 */
public class ContaBancaria {

    private Long id;                   // cd_conta
    private Long usuarioId;            // cd_usuario
    private Long bancoId;              // cd_banco
    private String bancoNome;          // nm_banco (vem do JOIN com tb_banco, só para exibir)
    private String agencia;            // nr_agencia (4 números)
    private String numeroConta;        // nr_conta (número com dígito, ex.: 12345-6)
    private BigDecimal saldo;          // vl_saldo
    private boolean dadosSimulados;    // st_dados_simulados ('S' = dados gerados pela API)
    private LocalDateTime dataVinculo; // dt_vinculo

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Long getBancoId() { return bancoId; }
    public void setBancoId(Long bancoId) { this.bancoId = bancoId; }

    public String getBancoNome() { return bancoNome; }
    public void setBancoNome(String bancoNome) { this.bancoNome = bancoNome; }

    public String getAgencia() { return agencia; }
    public void setAgencia(String agencia) { this.agencia = agencia; }

    public String getNumeroConta() { return numeroConta; }
    public void setNumeroConta(String numeroConta) { this.numeroConta = numeroConta; }

    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }

    public boolean isDadosSimulados() { return dadosSimulados; }
    public void setDadosSimulados(boolean dadosSimulados) { this.dadosSimulados = dadosSimulados; }

    public LocalDateTime getDataVinculo() { return dataVinculo; }
    public void setDataVinculo(LocalDateTime dataVinculo) { this.dataVinculo = dataVinculo; }
}
