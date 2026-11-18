package br.com.fiap.contacerta.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class CorsConfigTest {

    @Test
    void semFrontUrlLiberaSoOLocal() {
        assertEquals(List.of("http://localhost:5173"), new CorsConfig("").origensLiberadas());
    }

    @Test
    void aceitaVariosEnderecosSeparadosPorVirgulaSemBarraNoFinal() {
        CorsConfig config = new CorsConfig(" https://oficial.vercel.app/ , https://copia.vercel.app,, ");
        assertEquals(List.of("http://localhost:5173", "https://oficial.vercel.app", "https://copia.vercel.app"),
                config.origensLiberadas());
    }
}
