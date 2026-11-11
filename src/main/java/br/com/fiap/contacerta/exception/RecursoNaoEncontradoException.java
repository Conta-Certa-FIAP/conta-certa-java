package br.com.fiap.contacerta.exception;

/** O registro pedido não existe (ex.: usuário com id 999). Vira HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
