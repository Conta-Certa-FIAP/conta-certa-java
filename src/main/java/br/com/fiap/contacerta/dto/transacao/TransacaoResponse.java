package br.com.fiap.contacerta.dto.transacao;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Saída com os dados de uma transação (linha do extrato). */
public class TransacaoResponse {

    private Long id;
    private Long contaId;
    private Long categoriaId;
    private String categoriaNome;
    private Long cartaoId;
    private String descricao;
    private BigDecimal valor;
    private String tipo;      // ENTRADA ou SAIDA
    private String meio;      // PIX, TRANSFERENCIA, CARTAO_DEBITO, CARTAO_CREDITO, BOLETO, OUTRO
    private LocalDate data;
    private boolean simulada; // true = gerada pela API (dados simulados ou "simular compra")

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getContaId() { return contaId; }
    public void setContaId(Long contaId) { this.contaId = contaId; }

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }

    public String getCategoriaNome() { return categoriaNome; }
    public void setCategoriaNome(String categoriaNome) { this.categoriaNome = categoriaNome; }

    public Long getCartaoId() { return cartaoId; }
    public void setCartaoId(Long cartaoId) { this.cartaoId = cartaoId; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getMeio() { return meio; }
    public void setMeio(String meio) { this.meio = meio; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public boolean isSimulada() { return simulada; }
    public void setSimulada(boolean simulada) { this.simulada = simulada; }
}
