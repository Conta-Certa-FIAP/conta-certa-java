package br.com.fiap.contacerta.dao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Um "bloco de código" que roda dentro de uma transação JDBC e devolve um resultado.
 * É uma interface funcional: dá para passar uma lambda, ex.: con -> { ... return x; }
 */
@FunctionalInterface
public interface OperacaoTransacional<T> {

    T executar(Connection con) throws SQLException;
}
