package br.com.fiap.contacerta.dto.divida;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Saída com os dados de um pagamento. */
public class PagamentoResponse {

    private Long id;
    private Long dividaId;
    private Long contaId;
    private BigDecimal valor;
    private LocalDate data;

    /**
     * Só vem na resposta do POST: a dívida já atualizada (novo saldo, parcelas e status),
     * para o front não precisar fazer outra chamada.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DividaResponse divida;

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

    public DividaResponse getDivida() { return divida; }
    public void setDivida(DividaResponse divida) { this.divida = divida; }
}
