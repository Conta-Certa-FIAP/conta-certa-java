package br.com.fiap.contacerta.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Espelha a tabela tb_divida. */
public class Divida {

    private Long id;                          // cd_divida
    private Long usuarioId;                   // cd_usuario
    private Long contaId;                     // cd_conta (opcional)
    private String credor;                    // nm_credor
    private TipoDivida tipo;                  // tp_divida
    private BigDecimal valorOriginal;         // vl_original
    private BigDecimal saldoDevedor;          // vl_saldo_devedor (quanto ainda falta pagar)
    private BigDecimal taxaJurosMensal;       // tx_juros_mensal (em %, ex.: 13.5 = 13,5% ao mês)
    private Integer parcelasTotal;            // qt_parcelas_total
    private Integer parcelasPagas;            // qt_parcelas_pagas
    private BigDecimal valorParcela;          // vl_parcela
    private LocalDate dataContratacao;        // dt_contratacao
    private LocalDate dataProximoVencimento;  // dt_proximo_vencimento
    private Integer diasAtraso;               // nr_dias_atraso
    private StatusDivida status;              // st_divida
    private boolean simulada;                 // st_simulada

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Long getContaId() { return contaId; }
    public void setContaId(Long contaId) { this.contaId = contaId; }

    public String getCredor() { return credor; }
    public void setCredor(String credor) { this.credor = credor; }

    public TipoDivida getTipo() { return tipo; }
    public void setTipo(TipoDivida tipo) { this.tipo = tipo; }

    public BigDecimal getValorOriginal() { return valorOriginal; }
    public void setValorOriginal(BigDecimal valorOriginal) { this.valorOriginal = valorOriginal; }

    public BigDecimal getSaldoDevedor() { return saldoDevedor; }
    public void setSaldoDevedor(BigDecimal saldoDevedor) { this.saldoDevedor = saldoDevedor; }

    public BigDecimal getTaxaJurosMensal() { return taxaJurosMensal; }
    public void setTaxaJurosMensal(BigDecimal taxaJurosMensal) { this.taxaJurosMensal = taxaJurosMensal; }

    public Integer getParcelasTotal() { return parcelasTotal; }
    public void setParcelasTotal(Integer parcelasTotal) { this.parcelasTotal = parcelasTotal; }

    public Integer getParcelasPagas() { return parcelasPagas; }
    public void setParcelasPagas(Integer parcelasPagas) { this.parcelasPagas = parcelasPagas; }

    public BigDecimal getValorParcela() { return valorParcela; }
    public void setValorParcela(BigDecimal valorParcela) { this.valorParcela = valorParcela; }

    public LocalDate getDataContratacao() { return dataContratacao; }
    public void setDataContratacao(LocalDate dataContratacao) { this.dataContratacao = dataContratacao; }

    public LocalDate getDataProximoVencimento() { return dataProximoVencimento; }
    public void setDataProximoVencimento(LocalDate dataProximoVencimento) { this.dataProximoVencimento = dataProximoVencimento; }

    public Integer getDiasAtraso() { return diasAtraso; }
    public void setDiasAtraso(Integer diasAtraso) { this.diasAtraso = diasAtraso; }

    public StatusDivida getStatus() { return status; }
    public void setStatus(StatusDivida status) { this.status = status; }

    public boolean isSimulada() { return simulada; }
    public void setSimulada(boolean simulada) { this.simulada = simulada; }

    /** Dívida ativa = ainda não quitada (em dia ou atrasada). */
    public boolean ativa() {
        return status != StatusDivida.QUITADA;
    }
}
