package br.com.fiap.contacerta.dto.conta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Saída com os dados de uma conta bancária. */
public class ContaResponse {

    private Long id;
    private Long usuarioId;
    private Long bancoId;
    private String bancoNome;
    private String agencia;
    private String numeroConta;
    private BigDecimal saldo;
    private boolean dadosSimulados;
    private LocalDateTime dataVinculo;

    /** Só aparece na resposta do cadastro com importarDadosSimulados = true. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ImportacaoResumo importacao;

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

    public ImportacaoResumo getImportacao() { return importacao; }
    public void setImportacao(ImportacaoResumo importacao) { this.importacao = importacao; }
}
