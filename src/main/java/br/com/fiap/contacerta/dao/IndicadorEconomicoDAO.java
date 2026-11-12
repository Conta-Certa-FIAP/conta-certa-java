package br.com.fiap.contacerta.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import br.com.fiap.contacerta.exception.BancoDeDadosException;
import br.com.fiap.contacerta.model.IndicadorEconomico;

/** DAO da tabela tb_indicador_economico (JDBC puro): o cache dos dados do Banco Central. */
@Repository
public class IndicadorEconomicoDAO {

    private static final String SELECT = """
            SELECT cd_indicador, cd_serie_bcb, nm_indicador, dt_referencia, vl_indicador, dt_atualizacao
              FROM tb_indicador_economico
            """;

    private final DataSource dataSource;

    public IndicadorEconomicoDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Quando foi a última vez que buscamos esta série no Banco Central (vazio = nunca). */
    public Optional<LocalDateTime> ultimaAtualizacao(int codigoSerie) {
        String sql = "SELECT MAX(dt_atualizacao) AS dt_ultima FROM tb_indicador_economico WHERE cd_serie_bcb = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigoSerie);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return Optional.ofNullable(JdbcUtil.dataHora(rs, "dt_ultima"));
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao consultar a atualização dos indicadores", e);
        }
    }

    /**
     * Grava os valores SEM duplicar a data de uma série (constraint uk_tb_indicador_serie_dt).
     * O MERGE do Oracle faz "insere se não existe, atualiza se já existe" em um comando só.
     * Tudo vai em lote (batch) e numa transação: ou grava a série inteira, ou nada.
     */
    public void salvar(List<IndicadorEconomico> indicadores) {
        String sql = """
                MERGE INTO tb_indicador_economico i
                USING (SELECT ? AS cd_serie_bcb, ? AS dt_referencia FROM dual) novo
                   ON (i.cd_serie_bcb = novo.cd_serie_bcb AND i.dt_referencia = novo.dt_referencia)
                 WHEN MATCHED THEN
                      UPDATE SET i.vl_indicador = ?, i.dt_atualizacao = ?
                 WHEN NOT MATCHED THEN
                      INSERT (cd_indicador, cd_serie_bcb, nm_indicador, dt_referencia, vl_indicador, dt_atualizacao)
                      VALUES (sq_tb_indicador_economico.NEXTVAL, novo.cd_serie_bcb, ?, novo.dt_referencia, ?, ?)
                """;
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (IndicadorEconomico indicador : indicadores) {
                    Timestamp atualizacao = Timestamp.valueOf(indicador.getDataAtualizacao());
                    ps.setInt(1, indicador.getCodigoSerie());
                    ps.setDate(2, Date.valueOf(indicador.getDataReferencia()));
                    ps.setBigDecimal(3, indicador.getValor());
                    ps.setTimestamp(4, atualizacao);
                    ps.setString(5, indicador.getNome());
                    ps.setBigDecimal(6, indicador.getValor());
                    ps.setTimestamp(7, atualizacao);
                    ps.addBatch();
                }
                ps.executeBatch();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao salvar indicadores", e);
        }
    }

    /** O valor mais recente salvo de uma série. */
    public Optional<IndicadorEconomico> buscarMaisRecente(int codigoSerie) {
        String sql = SELECT + """
                 WHERE cd_serie_bcb = ?
                   AND dt_referencia = (SELECT MAX(dt_referencia) FROM tb_indicador_economico WHERE cd_serie_bcb = ?)
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigoSerie);
            ps.setInt(2, codigoSerie);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao buscar indicador", e);
        }
    }

    /** Valores salvos de uma série a partir de uma data, do mais antigo para o mais novo. */
    public List<IndicadorEconomico> listarDesde(int codigoSerie, LocalDate inicio) {
        String sql = SELECT + " WHERE cd_serie_bcb = ? AND dt_referencia >= ? ORDER BY dt_referencia";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, codigoSerie);
            ps.setDate(2, Date.valueOf(inicio));
            try (ResultSet rs = ps.executeQuery()) {
                List<IndicadorEconomico> indicadores = new ArrayList<>();
                while (rs.next()) {
                    indicadores.add(montar(rs));
                }
                return indicadores;
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao listar indicadores", e);
        }
    }

    private IndicadorEconomico montar(ResultSet rs) throws SQLException {
        IndicadorEconomico indicador = new IndicadorEconomico();
        indicador.setId(rs.getLong("cd_indicador"));
        indicador.setCodigoSerie(rs.getInt("cd_serie_bcb"));
        indicador.setNome(rs.getString("nm_indicador"));
        indicador.setDataReferencia(JdbcUtil.data(rs, "dt_referencia"));
        indicador.setValor(rs.getBigDecimal("vl_indicador"));
        indicador.setDataAtualizacao(JdbcUtil.dataHora(rs, "dt_atualizacao"));
        return indicador;
    }
}
