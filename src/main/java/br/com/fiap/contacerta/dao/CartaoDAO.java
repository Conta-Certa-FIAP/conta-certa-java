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
import br.com.fiap.contacerta.model.Cartao;
import br.com.fiap.contacerta.model.TipoCartao;

/** DAO da tabela tb_cartao (JDBC puro). */
@Repository
public class CartaoDAO {

    private static final String SELECT = """
            SELECT cd_cartao, cd_conta, tp_cartao, nr_final, vl_limite, vl_fatura_atual,
                   nr_dia_vencimento, dt_cadastro
              FROM tb_cartao
            """;

    private final DataSource dataSource;

    public CartaoDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void inserir(Connection con, Cartao cartao) throws SQLException {
        String sql = """
                INSERT INTO tb_cartao (cd_cartao, cd_conta, tp_cartao, nr_final, vl_limite,
                                       vl_fatura_atual, nr_dia_vencimento, dt_cadastro)
                VALUES (sq_tb_cartao.NEXTVAL, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql, new String[] {"cd_cartao"})) {
            ps.setLong(1, cartao.getContaId());
            ps.setString(2, cartao.getTipo().name());
            ps.setString(3, cartao.getNumeroFinal());
            ps.setBigDecimal(4, cartao.getLimite());
            ps.setBigDecimal(5, cartao.getFaturaAtual());
            JdbcUtil.setIntOuNull(ps, 6, cartao.getDiaVencimento());
            ps.setTimestamp(7, Timestamp.valueOf(cartao.getDataCadastro()));
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                chaves.next();
                cartao.setId(chaves.getLong(1));
            }
        }
    }

    /** Versão que abre a própria conexão (cadastro manual de um cartão, operação isolada). */
    public void inserir(Cartao cartao) {
        try (Connection con = dataSource.getConnection()) {
            inserir(con, cartao);
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao inserir cartão", e);
        }
    }

    public Optional<Cartao> buscarPorId(Long id) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE cd_cartao = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao buscar cartão", e);
        }
    }

    /** Busca travando a linha (FOR UPDATE): usado ao lançar compra no crédito, para a fatura não se perder. */
    public Optional<Cartao> buscarPorIdParaAtualizar(Connection con, Long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + " WHERE cd_cartao = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        }
    }

    /** Primeiro cartão do tipo pedido naquela conta (usado pelo "simular compra"). */
    public Optional<Cartao> buscarPorContaETipo(Connection con, Long contaId, TipoCartao tipo) throws SQLException {
        String sql = SELECT + " WHERE cd_conta = ? AND tp_cartao = ? ORDER BY cd_cartao";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, contaId);
            ps.setString(2, tipo.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        }
    }

    public List<Cartao> listarPorConta(Long contaId) {
        String sql = SELECT + " WHERE cd_conta = ? ORDER BY cd_cartao";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, contaId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Cartao> cartoes = new ArrayList<>();
                while (rs.next()) {
                    cartoes.add(montar(rs));
                }
                return cartoes;
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao listar cartões", e);
        }
    }

    /** Atualiza final, limite e vencimento. O tipo não muda e a fatura só muda pelas compras. */
    public void atualizar(Cartao cartao) {
        String sql = "UPDATE tb_cartao SET nr_final = ?, vl_limite = ?, nr_dia_vencimento = ? WHERE cd_cartao = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cartao.getNumeroFinal());
            ps.setBigDecimal(2, cartao.getLimite());
            JdbcUtil.setIntOuNull(ps, 3, cartao.getDiaVencimento());
            ps.setLong(4, cartao.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao atualizar cartão", e);
        }
    }

    /** Soma (ou subtrai, com valor negativo) na fatura. O GREATEST não deixa a fatura ficar negativa. */
    public void somarNaFatura(Connection con, Long cartaoId, BigDecimal valor) throws SQLException {
        String sql = "UPDATE tb_cartao SET vl_fatura_atual = GREATEST(vl_fatura_atual + ?, 0) WHERE cd_cartao = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, valor);
            ps.setLong(2, cartaoId);
            ps.executeUpdate();
        }
    }

    public void remover(Long id) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM tb_cartao WHERE cd_cartao = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao remover cartão", e);
        }
    }

    public void removerPorConta(Connection con, Long contaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM tb_cartao WHERE cd_conta = ?")) {
            ps.setLong(1, contaId);
            ps.executeUpdate();
        }
    }

    private Cartao montar(ResultSet rs) throws SQLException {
        Cartao cartao = new Cartao();
        cartao.setId(rs.getLong("cd_cartao"));
        cartao.setContaId(rs.getLong("cd_conta"));
        cartao.setTipo(TipoCartao.valueOf(rs.getString("tp_cartao")));
        cartao.setNumeroFinal(rs.getString("nr_final"));
        cartao.setLimite(rs.getBigDecimal("vl_limite"));
        cartao.setFaturaAtual(rs.getBigDecimal("vl_fatura_atual"));
        cartao.setDiaVencimento(JdbcUtil.intOuNull(rs, "nr_dia_vencimento"));
        cartao.setDataCadastro(JdbcUtil.dataHora(rs, "dt_cadastro"));
        return cartao;
    }
}
