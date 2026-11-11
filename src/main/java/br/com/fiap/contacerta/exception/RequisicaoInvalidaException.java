package br.com.fiap.contacerta.exception;

/**
 * A requisição veio com um dado inválido que o Bean Validation sozinho não pega
 * (ex.: mês fora do formato AAAA-MM, tipo da transação diferente do tipo da categoria).
 * Vira HTTP 400.
 */
public class RequisicaoInvalidaException extends RuntimeException {

    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
