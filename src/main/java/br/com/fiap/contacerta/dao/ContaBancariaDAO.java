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
import br.com.fiap.contacerta.model.ContaBancaria;

/**
 * DAO da tabela tb_conta_bancaria (JDBC puro).
 *
 * Dois tipos de método neste e nos próximos DAOs:
 *  - SEM Connection no parâmetro: abre a própria conexão, executa e fecha (operação isolada);
 *  - COM Connection no parâmetro: usa a conexão recebida, porque faz parte de uma transação
 *    maior controlada pelo ExecutorTransacao (quem dá commit/rollback é ele).
 */
@Repository
public class ContaBancariaDAO {

    // O JOIN traz o nome do banco para exibir junto com a conta.
    private static final String SELECT = """
            SELECT c.cd_conta, c.cd_usuario, c.cd_banco, b.nm_banco, c.nr_agencia, c.nr_conta,
                   c.vl_saldo, c.st_dados_simulados, c.dt_vinculo
              FROM tb_conta_bancaria c
              JOIN tb_banco b ON b.cd_banco = c.cd_banco
            """;

    private final DataSource dataSource;

    public ContaBancariaDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void inserir(Connection con, ContaBancaria conta) throws SQLException {
        String sql = """
                INSERT INTO tb_conta_bancaria (cd_conta, cd_usuario, cd_banco, nr_agencia, nr_conta,
                                               vl_saldo, st_dados_simulados, dt_vinculo)
                VALUES (sq_tb_conta_bancaria.NEXTVAL, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql, new String[] {"cd_conta"})) {
            ps.setLong(1, conta.getUsuarioId());
            ps.setLong(2, conta.getBancoId());
            ps.setString(3, conta.getAgencia());
            ps.setString(4, conta.getNumeroConta());
            ps.setBigDecimal(5, conta.getSaldo());
            ps.setString(6, JdbcUtil.simNao(conta.isDadosSimulados()));
            ps.setTimestamp(7, Timestamp.valueOf(conta.getDataVinculo()));
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                chaves.next();
                conta.setId(chaves.getLong(1));
            }
        }
    }

    public Optional<ContaBancaria> buscarPorId(Long id) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE c.cd_conta = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao buscar conta", e);
        }
    }

    /**
     * Busca a conta TRAVANDO a linha (FOR UPDATE) até o fim da transação.
     * Assim, se duas requisições tentarem gastar o mesmo saldo ao mesmo tempo, a segunda
     * espera a primeira terminar e já enxerga o saldo atualizado.
     */
    public Optional<ContaBancaria> buscarPorIdParaAtualizar(Connection con, Long id) throws SQLException {
        String sql = SELECT + " WHERE c.cd_conta = ? FOR UPDATE OF c.vl_saldo";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        }
    }

    public List<ContaBancaria> listarPorUsuario(Long usuarioId) {
        String sql = SELECT + " WHERE c.cd_usuario = ? ORDER BY c.cd_conta";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ContaBancaria> contas = new ArrayList<>();
                while (rs.next()) {
                    contas.add(montar(rs));
                }
                return contas;
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao listar contas", e);
        }
    }

    /**
     * Já existe essa agência+conta nesse banco? (constraint uk_tb_conta_numero)
     * "ignorarId" permite desconsiderar a própria conta numa atualização; passe null no cadastro.
     */
    public boolean existeNumero(Long bancoId, String agencia, String numeroConta, Long ignorarId) {
        String sql = """
                SELECT COUNT(*) FROM tb_conta_bancaria
                 WHERE cd_banco = ? AND nr_agencia = ? AND nr_conta = ? AND cd_conta <> ?
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, bancoId);
            ps.setString(2, agencia);
            ps.setString(3, numeroConta);
            ps.setLong(4, ignorarId == null ? -1L : ignorarId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao verificar conta", e);
        }
    }

    /** Atualiza agência e número. O saldo só muda pelas transações. */
    public void atualizarDados(ContaBancaria conta) {
        String sql = "UPDATE tb_conta_bancaria SET nr_agencia = ?, nr_conta = ? WHERE cd_conta = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, conta.getAgencia());
            ps.setString(2, conta.getNumeroConta());
            ps.setLong(3, conta.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao atualizar conta", e);
        }
    }

    /** Soma (valor positivo) ou subtrai (valor negativo) do saldo, direto no banco. */
    public void somarAoSaldo(Connection con, Long contaId, BigDecimal valor) throws SQLException {
        String sql = "UPDATE tb_conta_bancaria SET vl_saldo = vl_saldo + ? WHERE cd_conta = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, valor);
            ps.setLong(2, contaId);
            ps.executeUpdate();
        }
    }

    public void remover(Connection con, Long contaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM tb_conta_bancaria WHERE cd_conta = ?")) {
            ps.setLong(1, contaId);
            ps.executeUpdate();
        }
    }

    /** Soma dos saldos de todas as contas do usuário (0 se não tiver conta). */
    public BigDecimal somarSaldoPorUsuario(Long usuarioId) {
        String sql = "SELECT NVL(SUM(vl_saldo), 0) FROM tb_conta_bancaria WHERE cd_usuario = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao somar saldos", e);
        }
    }

    private ContaBancaria montar(ResultSet rs) throws SQLException {
        ContaBancaria conta = new ContaBancaria();
        conta.setId(rs.getLong("cd_conta"));
        conta.setUsuarioId(rs.getLong("cd_usuario"));
        conta.setBancoId(rs.getLong("cd_banco"));
        conta.setBancoNome(rs.getString("nm_banco"));
        conta.setAgencia(rs.getString("nr_agencia"));
        conta.setNumeroConta(rs.getString("nr_conta"));
        conta.setSaldo(rs.getBigDecimal("vl_saldo"));
        conta.setDadosSimulados(JdbcUtil.simNao(rs.getString("st_dados_simulados")));
        conta.setDataVinculo(JdbcUtil.dataHora(rs, "dt_vinculo"));
        return conta;
    }
}
