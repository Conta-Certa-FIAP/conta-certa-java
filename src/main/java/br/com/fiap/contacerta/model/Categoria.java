package br.com.fiap.contacerta.model;

/** Espelha a tabela tb_categoria: categorias de entradas e saídas. */
public class Categoria {

    private Long id;             // cd_categoria
    private String nome;         // nm_categoria
    private TipoMovimento tipo;  // tp_movimento
    private boolean essencial;   // st_essencial ('S'/'N')

    public Categoria() {
    }

    public Categoria(Long id, String nome, TipoMovimento tipo, boolean essencial) {
        this.id = id;
        this.nome = nome;
        this.tipo = tipo;
        this.essencial = essencial;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public TipoMovimento getTipo() { return tipo; }
    public void setTipo(TipoMovimento tipo) { this.tipo = tipo; }

    public boolean isEssencial() { return essencial; }
    public void setEssencial(boolean essencial) { this.essencial = essencial; }
}
