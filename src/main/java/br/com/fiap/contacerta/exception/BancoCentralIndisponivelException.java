package br.com.fiap.contacerta.exception;

/**
 * A chamada à API do Banco Central falhou (fora do ar, demorou mais de 10 segundos ou devolveu
 * algo inesperado). O IndicadorService captura esta exceção e tenta responder com o último dado
 * salvo no banco; só vira erro 503 para o usuário se não houver nada salvo.
 */
public class BancoCentralIndisponivelException extends RuntimeException {

    public BancoCentralIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public BancoCentralIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
