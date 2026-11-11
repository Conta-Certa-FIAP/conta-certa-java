package br.com.fiap.contacerta.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Espelha a tabela tb_cartao: cartão de crédito ou débito ligado a uma conta. */
public class Cartao {

    private Long id;                    // cd_cartao
    private Long contaId;               // cd_conta
    private TipoCartao tipo;            // tp_cartao
    private String numeroFinal;         // nr_final (4 últimos dígitos)
    private BigDecimal limite;          // vl_limite (obrigatório no crédito, nulo no débito)
    private BigDecimal faturaAtual;     // vl_fatura_atual (compras no crédito ainda não pagas)
    private Integer diaVencimento;      // nr_dia_vencimento (1 a 28)
    private LocalDateTime dataCadastro; // dt_cadastro

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getContaId() { return contaId; }
    public void setContaId(Long contaId) { this.contaId = contaId; }

    public TipoCartao getTipo() { return tipo; }
    public void setTipo(TipoCartao tipo) { this.tipo = tipo; }

    public String getNumeroFinal() { return numeroFinal; }
    public void setNumeroFinal(String numeroFinal) { this.numeroFinal = numeroFinal; }

    public BigDecimal getLimite() { return limite; }
    public void setLimite(BigDecimal limite) { this.limite = limite; }

    public BigDecimal getFaturaAtual() { return faturaAtual; }
    public void setFaturaAtual(BigDecimal faturaAtual) { this.faturaAtual = faturaAtual; }

    public Integer getDiaVencimento() { return diaVencimento; }
    public void setDiaVencimento(Integer diaVencimento) { this.diaVencimento = diaVencimento; }

    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }

    /** Quanto ainda dá para gastar no crédito: limite - fatura atual. No débito não se aplica (null). */
    public BigDecimal limiteDisponivel() {
        if (tipo != TipoCartao.CREDITO || limite == null) {
            return null;
        }
        BigDecimal fatura = faturaAtual == null ? BigDecimal.ZERO : faturaAtual;
        return limite.subtract(fatura);
    }
}
