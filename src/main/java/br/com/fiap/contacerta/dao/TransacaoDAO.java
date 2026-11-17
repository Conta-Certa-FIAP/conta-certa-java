package br.com.fiap.contacerta.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import br.com.fiap.contacerta.exception.BancoDeDadosException;
import br.com.fiap.contacerta.model.MeioPagamento;
import br.com.fiap.contacerta.model.TipoMovimento;
import br.com.fiap.contacerta.model.Transacao;

/** DAO da tabela tb_transacao (JDBC puro). */
@Repository
public class TransacaoDAO {

    private static final String SELECT = """
            SELECT t.cd_transacao, t.cd_conta, t.cd_categoria, cat.nm_categoria, t.cd_cartao,
                   t.ds_transacao, t.vl_transacao, t.tp_movimento, t.tp_meio, t.dt_transacao, t.st_simulada,
                   t.st_recorrente
              FROM tb_transacao t
              JOIN tb_categoria cat ON cat.cd_categoria = t.cd_categoria
            """;

    private static final String INSERT = """
            INSERT INTO tb_transacao (cd_transacao, cd_conta, cd_categoria, cd_cartao, ds_transacao,
                                      vl_transacao, tp_movimento, tp_meio, dt_transacao, st_simulada,
                                      st_recorrente)
            VALUES (sq_tb_transacao.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final DataSource dataSource;

    public TransacaoDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void inserir(Connection con, Transacao transacao) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT, new String[] {"cd_transacao"})) {
            preencher(ps, transacao);
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                chaves.next();
                transacao.setId(chaves.getLong(1));
            }
        }
    }

    /**
     * Insere várias transações de uma vez (batch). Usado na geração de dados simulados:
     * em vez de ~45 idas e voltas ao banco, os INSERTs são enviados em um único pacote.
     */
    public void inserirEmLote(Connection con, List<Transacao> transacoes) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT)) {
            for (Transacao transacao : transacoes) {
                preencher(ps, transacao);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public Optional<Transacao> buscarPorId(Long id) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + " WHERE t.cd_transacao = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao buscar transação", e);
        }
    }

    /**
     * Lista com filtros opcionais. O SQL é montado aos poucos, mas os VALORES sempre entram
     * como "?" (PreparedStatement), nunca concatenados no texto: isso evita SQL Injection.
     */
    public List<Transacao> listar(FiltroTransacao filtro) {
        StringBuilder sql = new StringBuilder(SELECT);
        List<Object> parametros = new ArrayList<>();

        if (filtro.usuarioId() != null) {
            sql.append(" JOIN tb_conta_bancaria c ON c.cd_conta = t.cd_conta WHERE c.cd_usuario = ?");
            parametros.add(filtro.usuarioId());
        } else {
            sql.append(" WHERE t.cd_conta = ?");
            parametros.add(filtro.contaId());
        }
        if (filtro.inicio() != null) {
            sql.append(" AND t.dt_transacao >= ?");
            parametros.add(Date.valueOf(filtro.inicio()));
        }
        if (filtro.fimExclusivo() != null) {
            sql.append(" AND t.dt_transacao < ?");
            parametros.add(Date.valueOf(filtro.fimExclusivo()));
        }
        if (filtro.tipo() != null) {
            sql.append(" AND t.tp_movimento = ?");
            parametros.add(filtro.tipo().name());
        }
        if (filtro.categoriaId() != null) {
            sql.append(" AND t.cd_categoria = ?");
            parametros.add(filtro.categoriaId());
        }
        sql.append(" ORDER BY t.dt_transacao DESC, t.cd_transacao DESC");

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Transacao> transacoes = new ArrayList<>();
                while (rs.next()) {
                    transacoes.add(montar(rs));
                }
                return transacoes;
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao listar transações", e);
        }
    }

    /**
     * Total movimentado por categoria, somando todas as contas do usuário, no período
     * [inicio, fimExclusivo). É a base de todos os cálculos da análise financeira.
     */
    public List<TotalCategoria> totaisPorCategoria(Long usuarioId, LocalDate inicio, LocalDate fimExclusivo) {
        String sql = """
                SELECT cat.cd_categoria, cat.nm_categoria, cat.tp_movimento, cat.st_essencial,
                       SUM(t.vl_transacao) AS vl_total
                  FROM tb_transacao t
                  JOIN tb_conta_bancaria c ON c.cd_conta = t.cd_conta
                  JOIN tb_categoria cat ON cat.cd_categoria = t.cd_categoria
                 WHERE c.cd_usuario = ? AND t.dt_transacao >= ? AND t.dt_transacao < ?
                 GROUP BY cat.cd_categoria, cat.nm_categoria, cat.tp_movimento, cat.st_essencial
                 ORDER BY vl_total DESC
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            ps.setDate(2, Date.valueOf(inicio));
            ps.setDate(3, Date.valueOf(fimExclusivo));
            try (ResultSet rs = ps.executeQuery()) {
                List<TotalCategoria> totais = new ArrayList<>();
                while (rs.next()) {
                    totais.add(new TotalCategoria(
                            rs.getLong("cd_categoria"),
                            rs.getString("nm_categoria"),
                            TipoMovimento.valueOf(rs.getString("tp_movimento")),
                            JdbcUtil.simNao(rs.getString("st_essencial")),
                            rs.getBigDecimal("vl_total")));
                }
                return totais;
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao totalizar transações por categoria", e);
        }
    }

    /** Data da transação mais antiga do usuário a partir de "inicio" (vazio se não houver nenhuma). */
    public Optional<LocalDate> primeiraDataDesde(Long usuarioId, LocalDate inicio) {
        String sql = """
                SELECT MIN(t.dt_transacao) AS dt_primeira
                  FROM tb_transacao t
                  JOIN tb_conta_bancaria c ON c.cd_conta = t.cd_conta
                 WHERE c.cd_usuario = ? AND t.dt_transacao >= ?
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            ps.setDate(2, Date.valueOf(inicio));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return Optional.ofNullable(JdbcUtil.data(rs, "dt_primeira"));
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao buscar a primeira transação", e);
        }
    }

    public int contarPorCartao(Long cartaoId) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM tb_transacao WHERE cd_cartao = ?")) {
            ps.setLong(1, cartaoId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao contar transações do cartão", e);
        }
    }

    public void remover(Connection con, Long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM tb_transacao WHERE cd_transacao = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void removerPorConta(Connection con, Long contaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM tb_transacao WHERE cd_conta = ?")) {
            ps.setLong(1, contaId);
            ps.executeUpdate();
        }
    }

    /** Preenche os "?" do INSERT (usado no insert simples e no lote). */
    private void preencher(PreparedStatement ps, Transacao transacao) throws SQLException {
        ps.setLong(1, transacao.getContaId());
        ps.setLong(2, transacao.getCategoriaId());
        JdbcUtil.setLongOuNull(ps, 3, transacao.getCartaoId());
        ps.setString(4, transacao.getDescricao());
        ps.setBigDecimal(5, transacao.getValor());
        ps.setString(6, transacao.getTipo().name());
        ps.setString(7, transacao.getMeio().name());
        ps.setDate(8, Date.valueOf(transacao.getData()));
        ps.setString(9, JdbcUtil.simNao(transacao.isSimulada()));
        ps.setString(10, JdbcUtil.simNao(transacao.isRecorrente()));
    }

    private Transacao montar(ResultSet rs) throws SQLException {
        Transacao transacao = new Transacao();
        transacao.setId(rs.getLong("cd_transacao"));
        transacao.setContaId(rs.getLong("cd_conta"));
        transacao.setCategoriaId(rs.getLong("cd_categoria"));
        transacao.setCategoriaNome(rs.getString("nm_categoria"));
        transacao.setCartaoId(JdbcUtil.longOuNull(rs, "cd_cartao"));
        transacao.setDescricao(rs.getString("ds_transacao"));
        transacao.setValor(rs.getBigDecimal("vl_transacao"));
        transacao.setTipo(TipoMovimento.valueOf(rs.getString("tp_movimento")));
        transacao.setMeio(MeioPagamento.valueOf(rs.getString("tp_meio")));
        transacao.setData(JdbcUtil.data(rs, "dt_transacao"));
        transacao.setSimulada(JdbcUtil.simNao(rs.getString("st_simulada")));
        transacao.setRecorrente(JdbcUtil.simNao(rs.getString("st_recorrente")));
        return transacao;
    }
}
