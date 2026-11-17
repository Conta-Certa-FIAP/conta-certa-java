package br.com.fiap.contacerta.exception;

/**
 * A requisição chegou sem token, ou com um token inválido/vencido.
 * Vira HTTP 401: o front apaga a sessão e manda a pessoa para a tela de login.
 */
public class NaoAutenticadoException extends RuntimeException {

    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
