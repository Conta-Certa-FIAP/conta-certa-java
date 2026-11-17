package br.com.fiap.contacerta.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pequenas conversões que se repetem em todos os DAOs.
 * O JDBC trabalha com java.sql.Date/Timestamp e com tipos primitivos (que não aceitam null);
 * o resto do projeto usa LocalDate/LocalDateTime e Long/Integer (que aceitam null).
 */
public final class JdbcUtil {

    private JdbcUtil() {
        // classe só com métodos estáticos: ninguém precisa dar "new"
    }

    /** Lê uma coluna numérica que pode ser NULL (rs.getLong devolveria 0 em vez de null). */
    public static Long longOuNull(ResultSet rs, String coluna) throws SQLException {
        long valor = rs.getLong(coluna);
        return rs.wasNull() ? null : valor;
    }

    public static Integer intOuNull(ResultSet rs, String coluna) throws SQLException {
        int valor = rs.getInt(coluna);
        return rs.wasNull() ? null : valor;
    }

    public static LocalDate data(ResultSet rs, String coluna) throws SQLException {
        Date data = rs.getDate(coluna);
        return data == null ? null : data.toLocalDate();
    }

    public static LocalDateTime dataHora(ResultSet rs, String coluna) throws SQLException {
        Timestamp dataHora = rs.getTimestamp(coluna);
        return dataHora == null ? null : dataHora.toLocalDateTime();
    }

    public static void setLongOuNull(PreparedStatement ps, int indice, Long valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.NUMERIC);
        } else {
            ps.setLong(indice, valor);
        }
    }

    public static void setIntOuNull(PreparedStatement ps, int indice, Integer valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.NUMERIC);
        } else {
            ps.setInt(indice, valor);
        }
    }

    public static void setDataOuNull(PreparedStatement ps, int indice, LocalDate valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.DATE);
        } else {
            ps.setDate(indice, Date.valueOf(valor));
        }
    }

    /**
     * true se o erro foi "valor repetido em chave primária/UNIQUE".
     * No Oracle é o ORA-00001 (código 1); o 23505 é o código padrão SQL usado por outros bancos.
     */
    public static boolean violouChaveUnica(SQLException e) {
        return e.getErrorCode() == 1 || "23505".equals(e.getSQLState());
    }

    /** O banco guarda verdadeiro/falso como 'S'/'N' (colunas st_...). */
    public static String simNao(boolean valor) {
        return valor ? "S" : "N";
    }

    public static boolean simNao(String valor) {
        return "S".equalsIgnoreCase(valor);
    }
}
