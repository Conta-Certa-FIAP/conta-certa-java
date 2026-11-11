package br.com.fiap.contacerta.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import br.com.fiap.contacerta.exception.BancoDeDadosException;
import br.com.fiap.contacerta.model.Pagamento;

/** DAO da tabela tb_pagamento (JDBC puro). */
@Repository
public class PagamentoDAO {

    private static final String SELECT =
            "SELECT cd_pagamento, cd_divida, cd_conta, vl_pagamento, dt_pagamento FROM tb_pagamento";

    private final DataSource dataSource;

    public PagamentoDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void inserir(Connection con, Pagamento pagamento) throws SQLException {
        String sql = """
                INSERT INTO tb_pagamento (cd_pagamento, cd_divida, cd_conta, vl_pagamento, dt_pagamento)
                VALUES (sq_tb_pagamento.NEXTVAL, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql, new String[] {"cd_pagamento"})) {
            ps.setLong(1, pagamento.getDividaId());
            JdbcUtil.setLongOuNull(ps, 2, pagamento.getContaId());
            ps.setBigDecimal(3, pagamento.getValor());
            ps.setDate(4, Date.valueOf(pagamento.getData()));
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                chaves.next();
                pagamento.setId(chaves.getLong(1));
            }
        }
    }

    public Optional<Pagamento> buscarPorId(Long id) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE cd_pagamento = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao buscar pagamento", e);
        }
    }

    public List<Pagamento> listarPorDivida(Long dividaId) {
        String sql = SELECT + " WHERE cd_divida = ? ORDER BY dt_pagamento DESC, cd_pagamento DESC";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, dividaId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Pagamento> pagamentos = new ArrayList<>();
                while (rs.next()) {
                    pagamentos.add(montar(rs));
                }
                return pagamentos;
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao listar pagamentos", e);
        }
    }

    public int contarPorDivida(Long dividaId) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM tb_pagamento WHERE cd_divida = ?")) {
            ps.setLong(1, dividaId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao contar pagamentos", e);
        }
    }

    /** Ao excluir uma conta bancária, o histórico de pagamentos é mantido, só perde o vínculo com a conta. */
    public void desvincularConta(Connection con, Long contaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE tb_pagamento SET cd_conta = NULL WHERE cd_conta = ?")) {
            ps.setLong(1, contaId);
            ps.executeUpdate();
        }
    }

    private Pagamento montar(ResultSet rs) throws SQLException {
        Pagamento pagamento = new Pagamento();
        pagamento.setId(rs.getLong("cd_pagamento"));
        pagamento.setDividaId(rs.getLong("cd_divida"));
        pagamento.setContaId(JdbcUtil.longOuNull(rs, "cd_conta"));
        pagamento.setValor(rs.getBigDecimal("vl_pagamento"));
        pagamento.setData(JdbcUtil.data(rs, "dt_pagamento"));
        return pagamento;
    }
}
