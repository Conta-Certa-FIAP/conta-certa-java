package br.com.fiap.contacerta.exception;

/** A mesma origem fez requisições demais em pouco tempo (ex.: formulário de contato). Vira HTTP 429. */
public class MuitasRequisicoesException extends RuntimeException {

    public MuitasRequisicoesException(String mensagem) {
        super(mensagem);
    }
}
