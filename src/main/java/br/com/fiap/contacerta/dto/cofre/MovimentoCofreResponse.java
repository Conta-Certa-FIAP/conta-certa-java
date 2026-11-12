package br.com.fiap.contacerta.dto.cofre;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Saída com os dados de um depósito ou resgate do cofre. */
public class MovimentoCofreResponse {

    private Long id;
    private Long cofreId;
    private Long contaId;
    private String tipo; // DEPOSITO ou RESGATE
    private BigDecimal valor;
    private LocalDateTime data;

    /** Só vem na resposta do POST: o cofre já atualizado (valor guardado e progresso da meta). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private CofreResponse cofre;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCofreId() { return cofreId; }
    public void setCofreId(Long cofreId) { this.cofreId = cofreId; }

    public Long getContaId() { return contaId; }
    public void setContaId(Long contaId) { this.contaId = contaId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }

    public CofreResponse getCofre() { return cofre; }
    public void setCofre(CofreResponse cofre) { this.cofre = cofre; }
}
