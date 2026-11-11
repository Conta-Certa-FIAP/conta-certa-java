package br.com.fiap.contacerta.dto.usuario;

/**
 * Saída de POST /auth/login.
 * Sessão simplificada: o front guarda este objeto no navegador e usa o "id" nas rotas
 * /usuarios/{id}/... Num sistema real o login devolveria um token (JWT) e a API conferiria
 * esse token em cada requisição.
 */
public class LoginResponse {

    private Long id;
    private String nome;
    private String email;
    private String cpfMascarado;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCpfMascarado() { return cpfMascarado; }
    public void setCpfMascarado(String cpfMascarado) { this.cpfMascarado = cpfMascarado; }
}
