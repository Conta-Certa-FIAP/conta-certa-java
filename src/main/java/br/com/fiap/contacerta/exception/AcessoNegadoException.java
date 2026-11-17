package br.com.fiap.contacerta.exception;

/**
 * A pessoa está logada, mas tentou mexer em algo que é de OUTRO usuário
 * (ex.: abrir /usuarios/7/dividas estando logada como o usuário 3). Vira HTTP 403.
 */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException() {
        super("Você não tem acesso a este recurso.");
    }
}
