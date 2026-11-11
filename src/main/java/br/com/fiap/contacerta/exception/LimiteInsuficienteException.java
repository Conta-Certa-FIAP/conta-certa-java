package br.com.fiap.contacerta.exception;

/** A compra no crédito passa do limite disponível (limite - fatura atual). HTTP 409. */
public class LimiteInsuficienteException extends RegraNegocioException {

    public LimiteInsuficienteException(String mensagem) {
        super(mensagem);
    }
}
