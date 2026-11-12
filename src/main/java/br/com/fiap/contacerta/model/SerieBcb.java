package br.com.fiap.contacerta.model;

/**
 * As séries do Banco Central (SGS) que a API usa.
 * Os códigos foram conferidos com requisições reais à API em novembro/2026.
 */
public enum SerieBcb {

    SELIC(432, "Selic (meta)", "% ao ano"),
    IPCA_MENSAL(433, "IPCA mensal", "% no mês"),
    IPCA_12_MESES(13522, "IPCA acumulado em 12 meses", "% em 12 meses"),
    DOLAR(1, "Dólar comercial (venda)", "R$");

    private final int codigo;
    private final String nome;
    private final String unidade;

    SerieBcb(int codigo, String nome, String unidade) {
        this.codigo = codigo;
        this.nome = nome;
        this.unidade = unidade;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getUnidade() {
        return unidade;
    }
}
