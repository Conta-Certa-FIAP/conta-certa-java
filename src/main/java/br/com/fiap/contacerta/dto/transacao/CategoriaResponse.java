package br.com.fiap.contacerta.dto.transacao;

/** Saída de GET /categorias. */
public class CategoriaResponse {

    private Long id;
    private String nome;
    private String tipo;        // ENTRADA ou SAIDA
    private boolean essencial;  // gasto essencial (mercado, moradia, saúde...)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public boolean isEssencial() { return essencial; }
    public void setEssencial(boolean essencial) { this.essencial = essencial; }
}
