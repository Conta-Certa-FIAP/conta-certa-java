package br.com.fiap.contacerta.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import br.com.fiap.contacerta.exception.BancoDeDadosException;
import br.com.fiap.contacerta.model.MensagemContato;

/** DAO da tabela tb_contato (JDBC puro): mensagens do formulário "Contato" do site. */
@Repository
public class ContatoDAO {

    private final DataSource dataSource;

    public ContatoDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void inserir(MensagemContato contato) {
        String sql = """
                INSERT INTO tb_contato (cd_contato, nm_remetente, ds_email, ds_mensagem, dt_envio)
                VALUES (sq_tb_contato.NEXTVAL, ?, ?, ?, ?)
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, contato.nome());
            ps.setString(2, contato.email());
            ps.setString(3, contato.mensagem());
            ps.setTimestamp(4, Timestamp.valueOf(contato.dataEnvio()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BancoDeDadosException("Erro ao gravar mensagem de contato", e);
        }
    }
}
