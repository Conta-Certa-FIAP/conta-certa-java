package br.com.fiap.contacerta.dao;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import br.com.fiap.contacerta.exception.BancoDeDadosException;

/**
 * Roda várias operações de banco como UMA transação JDBC ("tudo ou nada").
 *
 * Exemplo: registrar um pagamento mexe em 4 tabelas (pagamento, dívida, conta e transação).
 * Se a 3ª operação falhar, as duas primeiras precisam ser desfeitas, senão o banco fica
 * com dados pela metade. Passo a passo:
 *   1. setAutoCommit(false): o Oracle para de gravar a cada comando;
 *   2. roda o bloco recebendo a MESMA conexão (todos os DAOs usam essa conexão);
 *   3. deu certo -> commit() grava tudo de uma vez;
 *   4. deu erro  -> rollback() desfaz tudo e o erro continua subindo para o handler.
 */
@Component
public class ExecutorTransacao {

    private final DataSource dataSource;

    public ExecutorTransacao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> T executar(OperacaoTransacional<T> operacao) {
        // try-with-resources: a conexão é devolvida ao pool no fim, mesmo com erro
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            try {
                T resultado = operacao.executar(con);
                con.commit();
                return resultado;
            } catch (SQLException | RuntimeException erro) {
                con.rollback();
                throw erro;
            } finally {
                // A conexão volta para o pool e será reutilizada: deixamos como estava.
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao executar a transação no banco de dados", e);
        }
    }
}
