package br.com.fiap.contacerta.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Prepara o banco sozinho quando a API sobe, para ninguém precisar rodar SQL à mão:
 *
 *  - banco vazio (não existe tb_usuario) -> roda o database/script.sql inteiro;
 *  - banco da versão antiga (falta alguma coisa da v2) -> roda o database/migracao_v2.sql;
 *  - banco já atualizado -> não faz nada.
 *
 * Os dois arquivos .sql continuam sendo a fonte de verdade (o Maven os copia para dentro do .jar,
 * ver pom.xml). Para desligar, defina a variável de ambiente MIGRAR_BANCO=false.
 *
 * Roda antes de a API começar a atender (InitializingBean), então nenhuma requisição chega
 * com o banco pela metade.
 */
@Component
@DependsOnDatabaseInitialization
public class MigracaoBanco implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(MigracaoBanco.class);

    static final String SCRIPT_COMPLETO = "db/script.sql";
    static final String SCRIPT_V2 = "db/migracao_v2.sql";

    /**
     * Erros de "isso já existe" (coluna, tabela, sequence ou constraint). Na migração v2 eles
     * significam que aquela parte já tinha sido feita antes: dá para seguir para a próxima.
     * Oracle: ORA-00955, ORA-01430, ORA-02264, ORA-02275. H2 (usado nos testes locais):
     * 42101, 42121, 90035, 90045.
     */
    private static final Set<Integer> JA_EXISTE = Set.of(955, 1430, 2264, 2275, 42101, 42121, 90035, 90045);

    private final DataSource dataSource;
    private final boolean ligado;

    public MigracaoBanco(DataSource dataSource, @Value("${app.banco.migrar:true}") boolean ligado) {
        this.dataSource = dataSource;
        this.ligado = ligado;
    }

    @Override
    public void afterPropertiesSet() throws SQLException, IOException {
        if (!ligado) {
            log.info("Migração automática do banco desligada (MIGRAR_BANCO=false).");
            return;
        }
        try (Connection con = dataSource.getConnection()) {
            String esquema = con.getSchema(); // o usuário do Oracle: não olha tabelas de outros alunos
            if (!existeTabela(con, esquema, "TB_USUARIO")) {
                log.info("Banco vazio: criando as tabelas com {}.", SCRIPT_COMPLETO);
                executar(con, ler(SCRIPT_COMPLETO), false);
                return;
            }
            if (precisaDaV2(con, esquema)) {
                log.info("Banco da versão antiga: aplicando {}.", SCRIPT_V2);
                executar(con, ler(SCRIPT_V2), true);
                return;
            }
            log.info("Banco já está atualizado.");
        }
    }

    /** Falta qualquer peça da v2? (cada uma é conferida: uma migração interrompida no meio é retomada) */
    private boolean precisaDaV2(Connection con, String esquema) throws SQLException {
        return !existeColuna(con, esquema, "TB_DIVIDA", "NM_DIVIDA")
                || !existeColuna(con, esquema, "TB_TRANSACAO", "ST_RECORRENTE")
                || !existeTabela(con, esquema, "TB_PLANO_QUITACAO")
                || !existeTabela(con, esquema, "TB_LICAO_CONCLUIDA")
                || !existeTabela(con, esquema, "TB_CONTATO");
    }

    private static boolean existeTabela(Connection con, String esquema, String tabela) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();
        try (ResultSet rs = meta.getTables(null, esquema, tabela, new String[] {"TABLE"})) {
            return rs.next();
        }
    }

    private static boolean existeColuna(Connection con, String esquema, String tabela, String coluna)
            throws SQLException {
        try (ResultSet rs = con.getMetaData().getColumns(null, esquema, tabela, coluna)) {
            return rs.next();
        }
    }

    /**
     * Roda os comandos um a um. Os SELECTs de conferência e o COMMIT do arquivo são pulados
     * (a conexão já grava cada comando).
     *
     * @param toleraJaExiste true na migração: "já existe" quer dizer que aquela parte já foi feita
     */
    private static void executar(Connection con, String script, boolean toleraJaExiste) throws SQLException {
        int executados = 0;
        try (Statement st = con.createStatement()) {
            for (String comando : comandos(script)) {
                try {
                    st.execute(comando);
                    executados++;
                } catch (SQLException e) {
                    if (!toleraJaExiste || !JA_EXISTE.contains(e.getErrorCode())) {
                        throw e;
                    }
                    log.info("Já existia, pulando: {}", primeiraLinha(comando));
                }
            }
        }
        log.info("Banco preparado: {} comandos executados.", executados);
    }

    /**
     * Separa o arquivo .sql em comandos: tira as linhas de comentário (--), corta no ";"
     * e descarta os SELECTs de conferência e o COMMIT.
     * O ";" dentro de um texto entre aspas simples (ex.: num COMMENT ON TABLE) não corta o comando.
     */
    static List<String> comandos(String script) {
        StringBuilder semComentarios = new StringBuilder();
        for (String linha : script.split("\\R")) {
            if (!linha.trim().startsWith("--")) {
                semComentarios.append(linha).append('\n');
            }
        }

        List<String> comandos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean dentroDeTexto = false;
        for (char c : semComentarios.toString().toCharArray()) {
            if (c == '\'') {
                dentroDeTexto = !dentroDeTexto; // '' (aspas escapadas) entra e sai na sequência
            }
            if (c == ';' && !dentroDeTexto) {
                adicionar(comandos, atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        adicionar(comandos, atual.toString());
        return comandos;
    }

    private static void adicionar(List<String> comandos, String parte) {
        String comando = parte.trim();
        String maiusculo = comando.toUpperCase();
        if (!comando.isEmpty() && !maiusculo.startsWith("SELECT") && !maiusculo.equals("COMMIT")) {
            comandos.add(comando);
        }
    }

    private static String ler(String caminho) throws IOException {
        try (InputStream entrada = new ClassPathResource(caminho).getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String primeiraLinha(String comando) {
        int quebra = comando.indexOf('\n');
        return quebra < 0 ? comando : comando.substring(0, quebra);
    }
}
