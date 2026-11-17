package br.com.fiap.contacerta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import br.com.fiap.contacerta.exception.BancoDeDadosException;
import br.com.fiap.contacerta.model.Usuario;

/** DAO da tabela tb_usuario (JDBC puro). */
@Repository
public class UsuarioDAO {

    private static final String SELECT = """
            SELECT cd_usuario, nm_usuario, ds_email, nr_cpf, ds_senha_hash,
                   vl_renda_declarada, qt_horas_trabalho_mes, dt_cadastro
              FROM tb_usuario
            """;

    private final DataSource dataSource;

    public UsuarioDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Insere o usuário e preenche o id gerado pela sequence sq_tb_usuario. */
    public void inserir(Usuario usuario) {
        String sql = """
                INSERT INTO tb_usuario (cd_usuario, nm_usuario, ds_email, nr_cpf, ds_senha_hash,
                                        vl_renda_declarada, qt_horas_trabalho_mes, dt_cadastro)
                VALUES (sq_tb_usuario.NEXTVAL, ?, ?, ?, ?, ?, ?, ?)
                """;
        // O 2º parâmetro pede ao Oracle para devolver a coluna cd_usuario gerada pelo NEXTVAL.
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, new String[] {"cd_usuario"})) {
            ps.setString(1, usuario.getNome());
            ps.setString(2, usuario.getEmail());
            ps.setString(3, usuario.getCpf());
            ps.setString(4, usuario.getSenhaHash());
            ps.setBigDecimal(5, usuario.getRendaDeclarada());
            ps.setInt(6, usuario.getHorasTrabalhoMes());
            ps.setTimestamp(7, Timestamp.valueOf(usuario.getDataCadastro()));
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                chaves.next();
                usuario.setId(chaves.getLong(1));
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao inserir usuário", e);
        }
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return buscarUm(SELECT + " WHERE cd_usuario = ?", id);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return buscarUm(SELECT + " WHERE ds_email = ?", email);
    }

    public Optional<Usuario> buscarPorCpf(String cpf) {
        return buscarUm(SELECT + " WHERE nr_cpf = ?", cpf);
    }

    /** Atualiza os dados editáveis. CPF e senha não mudam por aqui. */
    public void atualizar(Usuario usuario) {
        String sql = """
                UPDATE tb_usuario
                   SET nm_usuario = ?, ds_email = ?, vl_renda_declarada = ?, qt_horas_trabalho_mes = ?
                 WHERE cd_usuario = ?
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario.getNome());
            ps.setString(2, usuario.getEmail());
            ps.setBigDecimal(3, usuario.getRendaDeclarada());
            ps.setInt(4, usuario.getHorasTrabalhoMes());
            ps.setLong(5, usuario.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao atualizar usuário", e);
        }
    }

    /** Remove dentro de uma transação maior (junto com o plano e as lições do usuário). */
    public void remover(Connection con, Long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM tb_usuario WHERE cd_usuario = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Conta quantos registros "filhos" o usuário tem (contas + dívidas + cofres).
     * Usado antes de excluir: usuário com filhos não pode ser removido (409).
     */
    public int contarVinculos(Long id) {
        String sql = """
                SELECT (SELECT COUNT(*) FROM tb_conta_bancaria WHERE cd_usuario = ?)
                     + (SELECT COUNT(*) FROM tb_divida         WHERE cd_usuario = ?)
                     + (SELECT COUNT(*) FROM tb_cofre          WHERE cd_usuario = ?) AS qt_vinculos
                  FROM dual
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, id);
            ps.setLong(3, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("qt_vinculos");
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao verificar vínculos do usuário", e);
        }
    }

    /** Executa um SELECT que devolve no máximo um usuário. O parâmetro pode ser Long ou String. */
    private Optional<Usuario> buscarUm(String sql, Object parametro) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao buscar usuário", e);
        }
    }

    private Usuario montar(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getLong("cd_usuario"));
        usuario.setNome(rs.getString("nm_usuario"));
        usuario.setEmail(rs.getString("ds_email"));
        usuario.setCpf(rs.getString("nr_cpf"));
        usuario.setSenhaHash(rs.getString("ds_senha_hash"));
        usuario.setRendaDeclarada(rs.getBigDecimal("vl_renda_declarada"));
        usuario.setHorasTrabalhoMes(rs.getInt("qt_horas_trabalho_mes"));
        usuario.setDataCadastro(JdbcUtil.dataHora(rs, "dt_cadastro"));
        return usuario;
    }
}
