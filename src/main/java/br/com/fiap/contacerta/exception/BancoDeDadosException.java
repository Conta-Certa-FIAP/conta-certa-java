package br.com.fiap.contacerta.exception;

import java.sql.SQLException;

/**
 * Embrulha o SQLException do JDBC numa exceção "não checada".
 * Assim os services não precisam de try/catch em todo lugar, e o handler global
 * decide a resposta (500 genérico, sem nunca mostrar o SQL para o usuário).
 */
public class BancoDeDadosException extends RuntimeException {

    public BancoDeDadosException(String mensagem, SQLException causa) {
        super(mensagem, causa);
    }

    /** Código do erro do Oracle (ex.: 1 = ORA-00001, valor duplicado em coluna UNIQUE). */
    public int getCodigoOracle() {
        return getCause() instanceof SQLException sql ? sql.getErrorCode() : 0;
    }
}
