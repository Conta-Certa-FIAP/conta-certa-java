package br.com.fiap.contacerta.model;

/**
 * Tipos de dívida, exatamente como no CHECK de tb_divida.tp_divida.
 * Cada tipo tem uma descrição amigável (campo "tipoDescricao" das respostas).
 */
public enum TipoDivida {
    CARTAO_ROTATIVO("Cartão de crédito (rotativo)"),
    CARTAO_PARCELADO("Cartão de crédito (parcelado)"),
    CHEQUE_ESPECIAL("Cheque especial"),
    EMPRESTIMO_PESSOAL("Empréstimo pessoal"),
    CONSIGNADO("Empréstimo consignado"),
    FINANCIAMENTO_VEICULO("Financiamento de veículo"),
    FINANCIAMENTO_IMOVEL("Financiamento imobiliário"),
    CREDIARIO("Crediário de loja"),
    CONTA_ATRASADA("Conta atrasada"),
    OUTRO("Outra dívida");

    private final String descricao;

    TipoDivida(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Crédito rotativo: não tem parcela fixa e cobra os juros mais altos do mercado.
     * Ter uma dívida dessas ativa já é sinal de atenção na análise financeira.
     */
    public boolean isRotativo() {
        return this == CARTAO_ROTATIVO || this == CHEQUE_ESPECIAL;
    }
}
