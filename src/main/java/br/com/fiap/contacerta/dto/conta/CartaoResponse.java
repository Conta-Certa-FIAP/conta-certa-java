package br.com.fiap.contacerta.dto.conta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Saída com os dados de um cartão. No débito, limite/limiteDisponivel/diaVencimento vêm nulos. */
public class CartaoResponse {

    private Long id;
    private Long contaId;
    private String tipo;
    private String numeroFinal;
    private BigDecimal limite;
    private BigDecimal faturaAtual;
    private BigDecimal limiteDisponivel; // calculado: limite - faturaAtual
    private Integer diaVencimento;
    private LocalDateTime dataCadastro;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getContaId() { return contaId; }
    public void setContaId(Long contaId) { this.contaId = contaId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getNumeroFinal() { return numeroFinal; }
    public void setNumeroFinal(String numeroFinal) { this.numeroFinal = numeroFinal; }

    public BigDecimal getLimite() { return limite; }
    public void setLimite(BigDecimal limite) { this.limite = limite; }

    public BigDecimal getFaturaAtual() { return faturaAtual; }
    public void setFaturaAtual(BigDecimal faturaAtual) { this.faturaAtual = faturaAtual; }

    public BigDecimal getLimiteDisponivel() { return limiteDisponivel; }
    public void setLimiteDisponivel(BigDecimal limiteDisponivel) { this.limiteDisponivel = limiteDisponivel; }

    public Integer getDiaVencimento() { return diaVencimento; }
    public void setDiaVencimento(Integer diaVencimento) { this.diaVencimento = diaVencimento; }

    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }
}
