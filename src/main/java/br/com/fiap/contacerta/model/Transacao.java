package br.com.fiap.contacerta.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Espelha a tabela tb_transacao: uma entrada ou saída de dinheiro (linha do extrato). */
public class Transacao {

    private Long id;               // cd_transacao
    private Long contaId;          // cd_conta
    private Long categoriaId;      // cd_categoria
    private String categoriaNome;  // nm_categoria (vem do JOIN com tb_categoria, só para exibir)
    private Long cartaoId;         // cd_cartao (só quando o meio é cartão)
    private String descricao;      // ds_transacao
    private BigDecimal valor;      // vl_transacao (sempre positivo; o tipo diz se entra ou sai)
    private TipoMovimento tipo;    // tp_movimento
    private MeioPagamento meio;    // tp_meio
    private LocalDate data;        // dt_transacao
    private boolean simulada;      // st_simulada ('S' = gerada pela API)
    private boolean recorrente;    // st_recorrente ('S' = a pessoa marcou "repetir todo mês")

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

    public TipoMovimento getTipo() { return tipo; }
    public void setTipo(TipoMovimento tipo) { this.tipo = tipo; }

    public MeioPagamento getMeio() { return meio; }
    public void setMeio(MeioPagamento meio) { this.meio = meio; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public boolean isSimulada() { return simulada; }
    public void setSimulada(boolean simulada) { this.simulada = simulada; }

    public boolean isRecorrente() { return recorrente; }
    public void setRecorrente(boolean recorrente) { this.recorrente = recorrente; }
}
