package br.com.fiap.contacerta.dto.cofre;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Saída com os dados de um cofre e o progresso da meta.
 * Os campos de progresso vêm nulos quando o cofre não tem meta (ou prazo).
 */
public class CofreResponse {

    private Long id;
    private Long usuarioId;
    private String nome;
    private BigDecimal valorMeta;
    private LocalDate dataLimite;
    private BigDecimal valorGuardado;
    private LocalDateTime dataCriacao;

    // ---- progresso da meta (calculado pela API) ----
    private BigDecimal percentualConcluido;   // 0 a 100
    private BigDecimal valorFaltante;         // quanto falta para a meta
    private Integer mesesRestantes;           // até a data limite
    private BigDecimal valorMensalNecessario; // quanto guardar por mês para bater a meta no prazo
    private boolean metaAtingida;
    private boolean prazoVencido;             // a data limite passou e a meta não foi batida

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

    public BigDecimal getPercentualConcluido() { return percentualConcluido; }
    public void setPercentualConcluido(BigDecimal percentualConcluido) { this.percentualConcluido = percentualConcluido; }

    public BigDecimal getValorFaltante() { return valorFaltante; }
    public void setValorFaltante(BigDecimal valorFaltante) { this.valorFaltante = valorFaltante; }

    public Integer getMesesRestantes() { return mesesRestantes; }
    public void setMesesRestantes(Integer mesesRestantes) { this.mesesRestantes = mesesRestantes; }

    public BigDecimal getValorMensalNecessario() { return valorMensalNecessario; }
    public void setValorMensalNecessario(BigDecimal valorMensalNecessario) { this.valorMensalNecessario = valorMensalNecessario; }

    public boolean isMetaAtingida() { return metaAtingida; }
    public void setMetaAtingida(boolean metaAtingida) { this.metaAtingida = metaAtingida; }

    public boolean isPrazoVencido() { return prazoVencido; }
    public void setPrazoVencido(boolean prazoVencido) { this.prazoVencido = prazoVencido; }
}
