package br.com.fiap.contacerta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import br.com.fiap.contacerta.exception.BancoDeDadosException;
import br.com.fiap.contacerta.model.PlanoSalvo;

/** DAO da tabela tb_plano_quitacao (JDBC puro): um plano por usuário. */
@Repository
public class PlanoDAO {

    private final DataSource dataSource;

    public PlanoDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<PlanoSalvo> buscarPorUsuario(Long usuarioId) {
        String sql = "SELECT cd_usuario, vl_mensal, tp_metodo FROM tb_plano_quitacao WHERE cd_usuario = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new PlanoSalvo(
                        rs.getLong("cd_usuario"), rs.getBigDecimal("vl_mensal"), rs.getString("tp_metodo")));
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao buscar o plano de quitação", e);
        }
    }

    /**
     * Grava o plano: tenta o UPDATE; se nenhuma linha mudou é porque o usuário ainda
     * não tinha plano, então faz o INSERT. As duas operações usam a mesma conexão.
     *
     * Duplo clique no "Salvar plano" do primeiro plano: as duas requisições podem não achar
     * linha e tentar o INSERT juntas. A segunda bate na chave primária; nesse caso o plano já
     * existe, então basta repetir o UPDATE.
     */
    public void salvar(PlanoSalvo plano) {
        String update = """
                UPDATE tb_plano_quitacao
                   SET vl_mensal = ?, tp_metodo = ?, dt_atualizacao = SYSDATE
                 WHERE cd_usuario = ?
                """;
        String insert = """
                INSERT INTO tb_plano_quitacao (vl_mensal, tp_metodo, dt_atualizacao, cd_usuario)
                VALUES (?, ?, SYSDATE, ?)
                """;
        try (Connection con = dataSource.getConnection()) {
            if (executar(con, update, plano) == 0) {
                try {
                    executar(con, insert, plano);
                } catch (SQLException e) {
                    if (!JdbcUtil.violouChaveUnica(e)) {
                        throw e;
                    }
                    executar(con, update, plano);
                }
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao salvar o plano de quitação", e);
        }
    }

    /** Usado ao excluir o usuário, dentro da mesma transação que apaga o usuário. */
    public void removerPorUsuario(Connection con, Long usuarioId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM tb_plano_quitacao WHERE cd_usuario = ?")) {
            ps.setLong(1, usuarioId);
            ps.executeUpdate();
        }
    }

    /** O UPDATE e o INSERT têm os "?" na mesma ordem: valor, método, usuário. */
    private int executar(Connection con, String sql, PlanoSalvo plano) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, plano.valorMensal());
            ps.setString(2, plano.metodo());
            ps.setLong(3, plano.usuarioId());
            return ps.executeUpdate();
        }
    }
}
