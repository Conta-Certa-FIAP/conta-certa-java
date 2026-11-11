package br.com.fiap.contacerta.exception;

/**
 * Login que não bate. Vira HTTP 401.
 * A mensagem é sempre genérica: não dizemos se o erro foi no e-mail/CPF ou na senha,
 * para não ajudar quem está tentando adivinhar contas de outras pessoas.
 */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail/CPF ou senha inválidos");
    }
}
