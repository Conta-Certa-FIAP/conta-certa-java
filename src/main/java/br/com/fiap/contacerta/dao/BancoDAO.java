package br.com.fiap.contacerta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import br.com.fiap.contacerta.exception.BancoDeDadosException;
import br.com.fiap.contacerta.model.Banco;

/**
 * DAO da tabela tb_banco (JDBC puro).
 * Padrão de todos os DAOs do projeto:
 *  - o DataSource (pool de conexões) chega pelo CONSTRUTOR;
 *  - todo SQL usa PreparedStatement com "?" (evita SQL Injection);
 *  - try-with-resources fecha conexão, statement e result set sozinho.
 */
@Repository
public class BancoDAO {

    private static final String SELECT = "SELECT cd_banco, nm_banco, nr_codigo_compe FROM tb_banco";

    private final DataSource dataSource;

    public BancoDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Banco> listar() {
        String sql = SELECT + " ORDER BY nm_banco";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Banco> bancos = new ArrayList<>();
            while (rs.next()) {
                bancos.add(montar(rs));
            }
            return bancos;
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao listar bancos", e);
        }
    }

    public Optional<Banco> buscarPorId(Long id) {
        String sql = SELECT + " WHERE cd_banco = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao buscar banco", e);
        }
    }

    /** Transforma a linha atual do ResultSet em um objeto Banco. */
    private Banco montar(ResultSet rs) throws SQLException {
        return new Banco(rs.getLong("cd_banco"), rs.getString("nm_banco"), rs.getString("nr_codigo_compe"));
    }
}
