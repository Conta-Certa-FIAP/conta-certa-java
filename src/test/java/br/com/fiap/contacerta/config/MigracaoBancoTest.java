package br.com.fiap.contacerta.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class MigracaoBancoTest {

    @Test
    void separaComandosTirandoComentariosSelectsECommit() {
        String script = """
                -- comentário; com ponto e vírgula
                CREATE TABLE tb_a (cd NUMBER(10));
                INSERT INTO tb_a VALUES (1);
                COMMIT;
                -- conferência
                SELECT COUNT(*) FROM tb_a;
                """;
        assertEquals(List.of("CREATE TABLE tb_a (cd NUMBER(10))", "INSERT INTO tb_a VALUES (1)"),
                MigracaoBanco.comandos(script));
    }

    @Test
    void pontoEVirgulaDentroDeTextoNaoCortaOComando() {
        String script = "COMMENT ON TABLE tb_a IS 'Entradas e saídas; outra parte';\nINSERT INTO tb_a VALUES ('d''Ávila');";
        assertEquals(List.of("COMMENT ON TABLE tb_a IS 'Entradas e saídas; outra parte'",
                "INSERT INTO tb_a VALUES ('d''Ávila')"), MigracaoBanco.comandos(script));
    }

    @Test
    void osScriptsDoRepositorioViramComandosValidos() throws IOException {
        List<String> completo = MigracaoBanco.comandos(
                Files.readString(Path.of("database/script.sql"), StandardCharsets.UTF_8));
        List<String> v2 = MigracaoBanco.comandos(
                Files.readString(Path.of("database/migracao_v2.sql"), StandardCharsets.UTF_8));

        // 12 sequences + 14 tabelas + 14 comentários + 8 bancos + 16 categorias
        assertEquals(64, completo.size());
        assertTrue(v2.stream().anyMatch(c -> c.startsWith("ALTER TABLE tb_divida ADD nm_divida")));
        assertTrue(v2.stream().noneMatch(c -> c.toUpperCase().startsWith("SELECT")));
    }
}
