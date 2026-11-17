package br.com.fiap.contacerta.dto.usuario;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;

/**
 * Entrada de POST /auth/login. O campo "login" aceita e-mail OU CPF.
 *
 * O @JsonAlias deixa o mesmo campo ser lido com outro nome no JSON: o front envia
 * {"email": "...", "password": "..."} e a API continua aceitando {"login": "...", "senha": "..."}.
 */
public class LoginRequest {

    @NotBlank(message = "Informe o e-mail ou o CPF")
    @JsonAlias("email")
    private String login;

    @NotBlank(message = "Informe a senha")
    @JsonAlias("password")
    private String senha;

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}
