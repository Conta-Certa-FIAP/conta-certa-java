package br.com.fiap.contacerta.dto.conta;

/** Saída de GET /bancos: um banco do catálogo. */
public class BancoResponse {

    private Long id;
    private String nome;
    private String codigoCompe;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCodigoCompe() { return codigoCompe; }
    public void setCodigoCompe(String codigoCompe) { this.codigoCompe = codigoCompe; }
}
