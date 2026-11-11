package br.com.fiap.contacerta.exception;

/** Um serviço externo (Banco Central) está fora do ar e não temos dado salvo para devolver. HTTP 503. */
public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
