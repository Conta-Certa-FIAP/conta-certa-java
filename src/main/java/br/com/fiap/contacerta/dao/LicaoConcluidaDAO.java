package br.com.fiap.contacerta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import br.com.fiap.contacerta.exception.BancoDeDadosException;

/**
 * DAO da tabela tb_licao_concluida (JDBC puro): quais lições das trilhas de aprendizado
 * cada usuário já concluiu. O texto das lições fica no CatalogoTrilhas; aqui só o progresso.
 */
@Repository
public class LicaoConcluidaDAO {

    private final DataSource dataSource;

    public LicaoConcluidaDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Ids das lições que o usuário já concluiu. */
    public Set<Long> listarPorUsuario(Long usuarioId) {
        String sql = "SELECT cd_licao FROM tb_licao_concluida WHERE cd_usuario = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                Set<Long> licoes = new HashSet<>();
                while (rs.next()) {
                    licoes.add(rs.getLong("cd_licao"));
                }
                return licoes;
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao listar lições concluídas", e);
        }
    }

    /**
     * Marca a lição como concluída. O "WHERE NOT EXISTS" faz o INSERT não acontecer se ela
     * já estava marcada: clicar duas vezes no botão não dá erro nem duplica a linha.
     * Se os dois cliques chegarem exatamente juntos, o segundo bate na chave primária;
     * como a lição já ficou marcada pelo primeiro, ignoramos esse erro.
     */
    public void marcar(Long usuarioId, Long licaoId) {
        String sql = """
                INSERT INTO tb_licao_concluida (cd_usuario, cd_licao, dt_conclusao)
                SELECT ?, ?, SYSDATE FROM dual
                 WHERE NOT EXISTS (SELECT 1 FROM tb_licao_concluida WHERE cd_usuario = ? AND cd_licao = ?)
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            ps.setLong(2, licaoId);
            ps.setLong(3, usuarioId);
            ps.setLong(4, licaoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (!JdbcUtil.violouChaveUnica(e)) {
                throw new BancoDeDadosException("Erro ao marcar lição como concluída", e);
            }
        }
    }

    /** Usado ao excluir o usuário, dentro da mesma transação que apaga o usuário. */
    public void removerPorUsuario(Connection con, Long usuarioId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM tb_licao_concluida WHERE cd_usuario = ?")) {
            ps.setLong(1, usuarioId);
            ps.executeUpdate();
        }
    }
}
