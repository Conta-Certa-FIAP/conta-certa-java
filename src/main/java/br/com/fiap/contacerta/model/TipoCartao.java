package br.com.fiap.contacerta.model;

/** Valores aceitos pelo CHECK de tb_cartao.tp_cartao. */
public enum TipoCartao {
    CREDITO("crédito"),
    DEBITO("débito");

    private final String descricao;

    TipoCartao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
