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
import br.com.fiap.contacerta.model.Categoria;
import br.com.fiap.contacerta.model.TipoMovimento;

/** DAO da tabela tb_categoria (JDBC puro). Só leitura: as categorias vêm do script do banco. */
@Repository
public class CategoriaDAO {

    private static final String SELECT =
            "SELECT cd_categoria, nm_categoria, tp_movimento, st_essencial FROM tb_categoria";

    private final DataSource dataSource;

    public CategoriaDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Categoria> listar() {
        String sql = SELECT + " ORDER BY tp_movimento, nm_categoria";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Categoria> categorias = new ArrayList<>();
            while (rs.next()) {
                categorias.add(montar(rs));
            }
            return categorias;
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao listar categorias", e);
        }
    }

    public Optional<Categoria> buscarPorId(Long id) {
        return buscarUm(SELECT + " WHERE cd_categoria = ?", id);
    }

    public Optional<Categoria> buscarPorNome(String nome) {
        return buscarUm(SELECT + " WHERE nm_categoria = ?", nome);
    }

    private Optional<Categoria> buscarUm(String sql, Object parametro) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao buscar categoria", e);
        }
    }

    private Categoria montar(ResultSet rs) throws SQLException {
        return new Categoria(
                rs.getLong("cd_categoria"),
                rs.getString("nm_categoria"),
                TipoMovimento.valueOf(rs.getString("tp_movimento")),
                JdbcUtil.simNao(rs.getString("st_essencial")));
    }
}
