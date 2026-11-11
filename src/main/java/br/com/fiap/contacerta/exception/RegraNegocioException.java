package br.com.fiap.contacerta.exception;

/**
 * Os dados estão bem formatados, mas quebram uma regra do negócio
 * (ex.: e-mail já cadastrado, pagamento maior que a dívida, excluir registro que tem filhos).
 * Vira HTTP 409 (Conflict).
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
