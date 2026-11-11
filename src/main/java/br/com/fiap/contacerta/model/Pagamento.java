package br.com.fiap.contacerta.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Espelha a tabela tb_pagamento: um pagamento feito para abater uma dívida. */
public class Pagamento {

    private Long id;           // cd_pagamento
    private Long dividaId;     // cd_divida
    private Long contaId;      // cd_conta (opcional: conta de onde saiu o dinheiro)
    private BigDecimal valor;  // vl_pagamento
    private LocalDate data;    // dt_pagamento

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDividaId() { return dividaId; }
    public void setDividaId(Long dividaId) { this.dividaId = dividaId; }

    public Long getContaId() { return contaId; }
    public void setContaId(Long contaId) { this.contaId = contaId; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
}
