package br.com.fiap.contacerta.dto.usuario;

import jakarta.validation.constraints.NotBlank;

/** Entrada de POST /auth/login. O campo "login" aceita e-mail OU CPF. */
public class LoginRequest {

    @NotBlank(message = "Informe o e-mail ou o CPF")
    private String login;

    @NotBlank(message = "Informe a senha")
    private String senha;

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}
