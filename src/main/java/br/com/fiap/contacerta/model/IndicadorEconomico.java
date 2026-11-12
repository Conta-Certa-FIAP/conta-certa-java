package br.com.fiap.contacerta.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Espelha a tabela tb_indicador_economico: cache dos indicadores do Banco Central. */
public class IndicadorEconomico {

    private Long id;                       // cd_indicador
    private Integer codigoSerie;           // cd_serie_bcb (código da série no Banco Central)
    private String nome;                   // nm_indicador
    private LocalDate dataReferencia;      // dt_referencia (data a que o valor se refere)
    private BigDecimal valor;              // vl_indicador
    private LocalDateTime dataAtualizacao; // dt_atualizacao (quando buscamos no Banco Central)

    public IndicadorEconomico() {
    }

    public IndicadorEconomico(Integer codigoSerie, String nome, LocalDate dataReferencia,
                              BigDecimal valor, LocalDateTime dataAtualizacao) {
        this.codigoSerie = codigoSerie;
        this.nome = nome;
        this.dataReferencia = dataReferencia;
        this.valor = valor;
        this.dataAtualizacao = dataAtualizacao;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getCodigoSerie() { return codigoSerie; }
    public void setCodigoSerie(Integer codigoSerie) { this.codigoSerie = codigoSerie; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public LocalDate getDataReferencia() { return dataReferencia; }
    public void setDataReferencia(LocalDate dataReferencia) { this.dataReferencia = dataReferencia; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
}
