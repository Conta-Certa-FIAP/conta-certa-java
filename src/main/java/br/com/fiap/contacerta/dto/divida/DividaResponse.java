package br.com.fiap.contacerta.dto.divida;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Saída com os dados de uma dívida. */
public class DividaResponse {

    private Long id;
    private Long usuarioId;
    private Long contaId;
    private String nome;               // apelido dado pela pessoa (pode vir nulo), ex.: "Cartão Banco A"
    private String credor;
    private String tipo;
    private String tipoDescricao;      // texto amigável, ex.: "Cartão de crédito (rotativo)"
    private BigDecimal valorOriginal;
    private BigDecimal saldoDevedor;
    private BigDecimal taxaJurosMensal; // % ao mês
    private Integer parcelasTotal;
    private Integer parcelasPagas;
    private Integer parcelasRestantes;  // calculado: total - pagas
    private BigDecimal valorParcela;
    private LocalDate dataContratacao;
    private LocalDate dataProximoVencimento;
    private Integer diasAtraso;
    private String status;              // EM_DIA, ATRASADA ou QUITADA
    private boolean simulada;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Long getContaId() { return contaId; }
    public void setContaId(Long contaId) { this.contaId = contaId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCredor() { return credor; }
    public void setCredor(String credor) { this.credor = credor; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTipoDescricao() { return tipoDescricao; }
    public void setTipoDescricao(String tipoDescricao) { this.tipoDescricao = tipoDescricao; }

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

    public Integer getParcelasRestantes() { return parcelasRestantes; }
    public void setParcelasRestantes(Integer parcelasRestantes) { this.parcelasRestantes = parcelasRestantes; }

    public BigDecimal getValorParcela() { return valorParcela; }
    public void setValorParcela(BigDecimal valorParcela) { this.valorParcela = valorParcela; }

    public LocalDate getDataContratacao() { return dataContratacao; }
    public void setDataContratacao(LocalDate dataContratacao) { this.dataContratacao = dataContratacao; }

    public LocalDate getDataProximoVencimento() { return dataProximoVencimento; }
    public void setDataProximoVencimento(LocalDate dataProximoVencimento) { this.dataProximoVencimento = dataProximoVencimento; }

    public Integer getDiasAtraso() { return diasAtraso; }
    public void setDiasAtraso(Integer diasAtraso) { this.diasAtraso = diasAtraso; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isSimulada() { return simulada; }
    public void setSimulada(boolean simulada) { this.simulada = simulada; }
}
