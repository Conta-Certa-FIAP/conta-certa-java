package br.com.fiap.contacerta.exception;

/** A conta não tem saldo para a operação (compra no débito, pagamento, depósito no cofre). HTTP 409. */
public class SaldoInsuficienteException extends RegraNegocioException {

    public SaldoInsuficienteException(String mensagem) {
        super(mensagem);
    }
}
