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
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.StatusDivida;
import br.com.fiap.contacerta.model.TipoDivida;

/** DAO da tabela tb_divida (JDBC puro). */
@Repository
public class DividaDAO {

    private static final String SELECT = """
            SELECT cd_divida, cd_usuario, cd_conta, nm_divida, nm_credor, tp_divida, vl_original, vl_saldo_devedor,
                   tx_juros_mensal, qt_parcelas_total, qt_parcelas_pagas, vl_parcela, dt_contratacao,
                   dt_proximo_vencimento, nr_dias_atraso, st_divida, st_simulada
              FROM tb_divida
            """;

    private final DataSource dataSource;

    public DividaDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void inserir(Connection con, Divida divida) throws SQLException {
        String sql = """
                INSERT INTO tb_divida (cd_divida, cd_usuario, cd_conta, nm_credor, tp_divida, vl_original,
                                       vl_saldo_devedor, tx_juros_mensal, qt_parcelas_total, qt_parcelas_pagas,
                                       vl_parcela, dt_contratacao, dt_proximo_vencimento, nr_dias_atraso,
                                       st_divida, st_simulada, nm_divida)
                VALUES (sq_tb_divida.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql, new String[] {"cd_divida"})) {
            ps.setLong(1, divida.getUsuarioId());
            JdbcUtil.setLongOuNull(ps, 2, divida.getContaId());
            ps.setString(3, divida.getCredor());
            ps.setString(4, divida.getTipo().name());
            ps.setBigDecimal(5, divida.getValorOriginal());
            ps.setBigDecimal(6, divida.getSaldoDevedor());
            ps.setBigDecimal(7, divida.getTaxaJurosMensal());
            ps.setInt(8, divida.getParcelasTotal());
            ps.setInt(9, divida.getParcelasPagas());
            ps.setBigDecimal(10, divida.getValorParcela());
            ps.setDate(11, Date.valueOf(divida.getDataContratacao()));
            JdbcUtil.setDataOuNull(ps, 12, divida.getDataProximoVencimento());
            ps.setInt(13, divida.getDiasAtraso());
            ps.setString(14, divida.getStatus().name());
            ps.setString(15, JdbcUtil.simNao(divida.isSimulada()));
            ps.setString(16, divida.getNome()); // pode ser null: a coluna aceita
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                chaves.next();
                divida.setId(chaves.getLong(1));
            }
        }
    }

    public void inserir(Divida divida) {
        try (Connection con = dataSource.getConnection()) {
            inserir(con, divida);
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao inserir dívida", e);
        }
    }

    public Optional<Divida> buscarPorId(Long id) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE cd_divida = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao buscar dívida", e);
        }
    }

    /** Busca travando a linha (FOR UPDATE): usado ao registrar pagamento. */
    public Optional<Divida> buscarPorIdParaAtualizar(Connection con, Long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + " WHERE cd_divida = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        }
    }

    public List<Divida> listarPorUsuario(Long usuarioId) {
        String sql = SELECT + " WHERE cd_usuario = ? ORDER BY cd_divida";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Divida> dividas = new ArrayList<>();
                while (rs.next()) {
                    dividas.add(montar(rs));
                }
                return dividas;
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao listar dívidas", e);
        }
    }

    public void atualizar(Connection con, Divida divida) throws SQLException {
        String sql = """
                UPDATE tb_divida
                   SET cd_conta = ?, nm_credor = ?, tp_divida = ?, vl_original = ?, vl_saldo_devedor = ?,
                       tx_juros_mensal = ?, qt_parcelas_total = ?, qt_parcelas_pagas = ?, vl_parcela = ?,
                       dt_contratacao = ?, dt_proximo_vencimento = ?, nr_dias_atraso = ?, st_divida = ?,
                       nm_divida = ?
                 WHERE cd_divida = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            JdbcUtil.setLongOuNull(ps, 1, divida.getContaId());
            ps.setString(2, divida.getCredor());
            ps.setString(3, divida.getTipo().name());
            ps.setBigDecimal(4, divida.getValorOriginal());
            ps.setBigDecimal(5, divida.getSaldoDevedor());
            ps.setBigDecimal(6, divida.getTaxaJurosMensal());
            ps.setInt(7, divida.getParcelasTotal());
            ps.setInt(8, divida.getParcelasPagas());
            ps.setBigDecimal(9, divida.getValorParcela());
            ps.setDate(10, Date.valueOf(divida.getDataContratacao()));
            JdbcUtil.setDataOuNull(ps, 11, divida.getDataProximoVencimento());
            ps.setInt(12, divida.getDiasAtraso());
            ps.setString(13, divida.getStatus().name());
            ps.setString(14, divida.getNome());
            ps.setLong(15, divida.getId());
            ps.executeUpdate();
        }
    }

    public void atualizar(Divida divida) {
        try (Connection con = dataSource.getConnection()) {
            atualizar(con, divida);
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao atualizar dívida", e);
        }
    }

    public void remover(Long id) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM tb_divida WHERE cd_divida = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao remover dívida", e);
        }
    }

    /** Ao excluir uma conta bancária, as dívidas ligadas a ela continuam existindo, só perdem o vínculo. */
    public void desvincularConta(Connection con, Long contaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE tb_divida SET cd_conta = NULL WHERE cd_conta = ?")) {
            ps.setLong(1, contaId);
            ps.executeUpdate();
        }
    }

    private Divida montar(ResultSet rs) throws SQLException {
        Divida divida = new Divida();
        divida.setId(rs.getLong("cd_divida"));
        divida.setUsuarioId(rs.getLong("cd_usuario"));
        divida.setContaId(JdbcUtil.longOuNull(rs, "cd_conta"));
        divida.setNome(rs.getString("nm_divida"));
        divida.setCredor(rs.getString("nm_credor"));
        divida.setTipo(TipoDivida.valueOf(rs.getString("tp_divida")));
        divida.setValorOriginal(rs.getBigDecimal("vl_original"));
        divida.setSaldoDevedor(rs.getBigDecimal("vl_saldo_devedor"));
        divida.setTaxaJurosMensal(rs.getBigDecimal("tx_juros_mensal"));
        divida.setParcelasTotal(rs.getInt("qt_parcelas_total"));
        divida.setParcelasPagas(rs.getInt("qt_parcelas_pagas"));
        divida.setValorParcela(rs.getBigDecimal("vl_parcela"));
        divida.setDataContratacao(JdbcUtil.data(rs, "dt_contratacao"));
        divida.setDataProximoVencimento(JdbcUtil.data(rs, "dt_proximo_vencimento"));
        divida.setDiasAtraso(rs.getInt("nr_dias_atraso"));
        divida.setStatus(StatusDivida.valueOf(rs.getString("st_divida")));
        divida.setSimulada(JdbcUtil.simNao(rs.getString("st_simulada")));
        return divida;
    }
}
