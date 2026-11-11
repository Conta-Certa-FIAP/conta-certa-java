package br.com.fiap.contacerta.model;

/** Espelha a tabela tb_banco: catálogo de bancos que o usuário pode "conectar". */
public class Banco {

    private Long id;             // cd_banco
    private String nome;         // nm_banco
    private String codigoCompe;  // nr_codigo_compe (código oficial de 3 dígitos do banco)

    public Banco() {
    }

    public Banco(Long id, String nome, String codigoCompe) {
        this.id = id;
        this.nome = nome;
        this.codigoCompe = codigoCompe;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCodigoCompe() { return codigoCompe; }
    public void setCodigoCompe(String codigoCompe) { this.codigoCompe = codigoCompe; }
}
