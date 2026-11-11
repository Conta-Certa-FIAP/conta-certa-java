package br.com.fiap.contacerta.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Espelha a tabela tb_movimento_cofre: um depósito ou resgate de um cofre. */
public class MovimentoCofre {

    private Long id;                  // cd_movimento
    private Long cofreId;             // cd_cofre
    private Long contaId;             // cd_conta (de onde saiu ou para onde voltou o dinheiro)
    private TipoMovimentoCofre tipo;  // tp_movimento
    private BigDecimal valor;         // vl_movimento
    private LocalDateTime data;       // dt_movimento

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCofreId() { return cofreId; }
    public void setCofreId(Long cofreId) { this.cofreId = cofreId; }

    public Long getContaId() { return contaId; }
    public void setContaId(Long contaId) { this.contaId = contaId; }

    public TipoMovimentoCofre getTipo() { return tipo; }
    public void setTipo(TipoMovimentoCofre tipo) { this.tipo = tipo; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }
}
