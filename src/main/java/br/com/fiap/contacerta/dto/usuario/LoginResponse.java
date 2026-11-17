package br.com.fiap.contacerta.dto.usuario;

/**
 * Saída de POST /auth/login.
 *
 * O front guarda este objeto no navegador. O "token" volta em todas as próximas requisições,
 * no cabeçalho "Authorization: Bearer <token>", e é por ele que a API sabe quem está logado
 * (ver TokenService). O "id" serve para montar as rotas /usuarios/{id}/...
 *
 * "userName" repete o nome com a chave que o front espera (ver ENDPOINTS.md do front).
 */
public class LoginResponse {

    private Long id;
    private String nome;
    private String email;
    private String cpfMascarado;
    private String token;
    private String userName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCpfMascarado() { return cpfMascarado; }
    public void setCpfMascarado(String cpfMascarado) { this.cpfMascarado = cpfMascarado; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}
