package br.com.fiap.contacerta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import br.com.fiap.contacerta.exception.BancoDeDadosException;
import br.com.fiap.contacerta.model.MovimentoCofre;
import br.com.fiap.contacerta.model.TipoMovimentoCofre;

/** DAO da tabela tb_movimento_cofre (JDBC puro). */
@Repository
public class MovimentoCofreDAO {

    private static final String SELECT = """
            SELECT cd_movimento, cd_cofre, cd_conta, tp_movimento, vl_movimento, dt_movimento
              FROM tb_movimento_cofre
            """;

    private final DataSource dataSource;

    public MovimentoCofreDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void inserir(Connection con, MovimentoCofre movimento) throws SQLException {
        String sql = """
                INSERT INTO tb_movimento_cofre (cd_movimento, cd_cofre, cd_conta, tp_movimento, vl_movimento, dt_movimento)
                VALUES (sq_tb_movimento_cofre.NEXTVAL, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql, new String[] {"cd_movimento"})) {
            ps.setLong(1, movimento.getCofreId());
            ps.setLong(2, movimento.getContaId());
            ps.setString(3, movimento.getTipo().name());
            ps.setBigDecimal(4, movimento.getValor());
            ps.setTimestamp(5, Timestamp.valueOf(movimento.getData()));
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                chaves.next();
                movimento.setId(chaves.getLong(1));
            }
        }
    }

    public Optional<MovimentoCofre> buscarPorId(Long id) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE cd_movimento = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao buscar movimento do cofre", e);
        }
    }

    public List<MovimentoCofre> listarPorCofre(Long cofreId) {
        String sql = SELECT + " WHERE cd_cofre = ? ORDER BY dt_movimento DESC, cd_movimento DESC";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, cofreId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MovimentoCofre> movimentos = new ArrayList<>();
                while (rs.next()) {
                    movimentos.add(montar(rs));
                }
                return movimentos;
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao listar movimentos do cofre", e);
        }
    }

    public void removerPorConta(Connection con, Long contaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM tb_movimento_cofre WHERE cd_conta = ?")) {
            ps.setLong(1, contaId);
            ps.executeUpdate();
        }
    }

    public void removerPorCofre(Connection con, Long cofreId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM tb_movimento_cofre WHERE cd_cofre = ?")) {
            ps.setLong(1, cofreId);
            ps.executeUpdate();
        }
    }

    private MovimentoCofre montar(ResultSet rs) throws SQLException {
        MovimentoCofre movimento = new MovimentoCofre();
        movimento.setId(rs.getLong("cd_movimento"));
        movimento.setCofreId(rs.getLong("cd_cofre"));
        movimento.setContaId(rs.getLong("cd_conta"));
        movimento.setTipo(TipoMovimentoCofre.valueOf(rs.getString("tp_movimento")));
        movimento.setValor(rs.getBigDecimal("vl_movimento"));
        movimento.setData(JdbcUtil.dataHora(rs, "dt_movimento"));
        return movimento;
    }
}
