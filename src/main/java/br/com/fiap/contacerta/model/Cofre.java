package br.com.fiap.contacerta.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Espelha a tabela tb_cofre: um cofre/meta de economia do usuário. */
public class Cofre {

    private Long id;                   // cd_cofre
    private Long usuarioId;            // cd_usuario
    private String nome;               // nm_cofre
    private BigDecimal valorMeta;      // vl_meta (opcional)
    private LocalDate dataLimite;      // dt_limite (opcional: prazo da meta)
    private BigDecimal valorGuardado;  // vl_guardado
    private LocalDateTime dataCriacao; // dt_criacao

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public BigDecimal getValorMeta() { return valorMeta; }
    public void setValorMeta(BigDecimal valorMeta) { this.valorMeta = valorMeta; }

    public LocalDate getDataLimite() { return dataLimite; }
    public void setDataLimite(LocalDate dataLimite) { this.dataLimite = dataLimite; }

    public BigDecimal getValorGuardado() { return valorGuardado; }
    public void setValorGuardado(BigDecimal valorGuardado) { this.valorGuardado = valorGuardado; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
}
