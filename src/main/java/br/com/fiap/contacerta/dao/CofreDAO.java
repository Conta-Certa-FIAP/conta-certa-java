package br.com.fiap.contacerta.dao;

import java.math.BigDecimal;
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
import br.com.fiap.contacerta.model.Cofre;

/** DAO da tabela tb_cofre (JDBC puro). */
@Repository
public class CofreDAO {

    private static final String SELECT = """
            SELECT cd_cofre, cd_usuario, nm_cofre, vl_meta, dt_limite, vl_guardado, dt_criacao
              FROM tb_cofre
            """;

    private final DataSource dataSource;

    public CofreDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void inserir(Cofre cofre) {
        String sql = """
                INSERT INTO tb_cofre (cd_cofre, cd_usuario, nm_cofre, vl_meta, dt_limite, vl_guardado, dt_criacao)
                VALUES (sq_tb_cofre.NEXTVAL, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, new String[] {"cd_cofre"})) {
            ps.setLong(1, cofre.getUsuarioId());
            ps.setString(2, cofre.getNome());
            ps.setBigDecimal(3, cofre.getValorMeta());
            JdbcUtil.setDataOuNull(ps, 4, cofre.getDataLimite());
            ps.setBigDecimal(5, cofre.getValorGuardado());
            ps.setTimestamp(6, Timestamp.valueOf(cofre.getDataCriacao()));
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                chaves.next();
                cofre.setId(chaves.getLong(1));
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao inserir cofre", e);
        }
    }

    public Optional<Cofre> buscarPorId(Long id) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE cd_cofre = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao buscar cofre", e);
        }
    }

    /** Busca travando a linha (FOR UPDATE): usado em depósito e resgate. */
    public Optional<Cofre> buscarPorIdParaAtualizar(Connection con, Long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + " WHERE cd_cofre = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        }
    }

    public List<Cofre> listarPorUsuario(Long usuarioId) {
        String sql = SELECT + " WHERE cd_usuario = ? ORDER BY cd_cofre";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Cofre> cofres = new ArrayList<>();
                while (rs.next()) {
                    cofres.add(montar(rs));
                }
                return cofres;
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao listar cofres", e);
        }
    }

    /** Atualiza nome, meta e prazo. O valor guardado só muda por depósito e resgate. */
    public void atualizar(Cofre cofre) {
        String sql = "UPDATE tb_cofre SET nm_cofre = ?, vl_meta = ?, dt_limite = ? WHERE cd_cofre = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cofre.getNome());
            ps.setBigDecimal(2, cofre.getValorMeta());
            JdbcUtil.setDataOuNull(ps, 3, cofre.getDataLimite());
            ps.setLong(4, cofre.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao atualizar cofre", e);
        }
    }

    /** Soma (depósito) ou subtrai (resgate, valor negativo) do valor guardado. */
    public void somarAoGuardado(Connection con, Long cofreId, BigDecimal valor) throws SQLException {
        String sql = "UPDATE tb_cofre SET vl_guardado = vl_guardado + ? WHERE cd_cofre = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, valor);
            ps.setLong(2, cofreId);
            ps.executeUpdate();
        }
    }

    public void remover(Connection con, Long cofreId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM tb_cofre WHERE cd_cofre = ?")) {
            ps.setLong(1, cofreId);
            ps.executeUpdate();
        }
    }

    /** Total guardado em todos os cofres do usuário (0 se não tiver cofre). */
    public BigDecimal somarGuardadoPorUsuario(Long usuarioId) {
        String sql = "SELECT NVL(SUM(vl_guardado), 0) FROM tb_cofre WHERE cd_usuario = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao somar cofres", e);
        }
    }

    private Cofre montar(ResultSet rs) throws SQLException {
        Cofre cofre = new Cofre();
        cofre.setId(rs.getLong("cd_cofre"));
        cofre.setUsuarioId(rs.getLong("cd_usuario"));
        cofre.setNome(rs.getString("nm_cofre"));
        cofre.setValorMeta(rs.getBigDecimal("vl_meta"));
        cofre.setDataLimite(JdbcUtil.data(rs, "dt_limite"));
        cofre.setValorGuardado(rs.getBigDecimal("vl_guardado"));
        cofre.setDataCriacao(JdbcUtil.dataHora(rs, "dt_criacao"));
        return cofre;
    }
}
