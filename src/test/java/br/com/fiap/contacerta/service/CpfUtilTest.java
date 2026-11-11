package br.com.fiap.contacerta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Testes do validador e da máscara de CPF (não dependem do banco). */
class CpfUtilTest {

    @Test
    void aceitaCpfValidoComESemMascara() {
        assertTrue(CpfUtil.valido("529.982.247-25"));
        assertTrue(CpfUtil.valido("52998224725"));
        assertTrue(CpfUtil.valido("111.444.777-35"));
    }

    @Test
    void rejeitaDigitoVerificadorErrado() {
        assertFalse(CpfUtil.valido("529.982.247-26"));
        assertFalse(CpfUtil.valido("52998224735"));
    }

    @Test
    void rejeitaSequenciasRepetidas() {
        assertFalse(CpfUtil.valido("111.111.111-11"));
        assertFalse(CpfUtil.valido("00000000000"));
        assertFalse(CpfUtil.valido("99999999999"));
    }

    @Test
    void rejeitaFormatoErrado() {
        assertFalse(CpfUtil.valido(null));
        assertFalse(CpfUtil.valido(""));
        assertFalse(CpfUtil.valido("123"));
        assertFalse(CpfUtil.valido("529.982.247-2"));
        assertFalse(CpfUtil.valido("abc52998224725"));
        assertFalse(CpfUtil.valido("529982247-25x"));
    }

    @Test
    void guardaSomenteOsNumeros() {
        assertEquals("52998224725", CpfUtil.apenasNumeros("529.982.247-25"));
        assertEquals("52998224725", CpfUtil.apenasNumeros(" 52998224725 "));
    }

    @Test
    void mascaraEscondeInicioEFim() {
        assertEquals("***.982.247-**", CpfUtil.mascarar("52998224725"));
        assertEquals("***.456.789-**", CpfUtil.mascarar("12345678909"));
    }
}
