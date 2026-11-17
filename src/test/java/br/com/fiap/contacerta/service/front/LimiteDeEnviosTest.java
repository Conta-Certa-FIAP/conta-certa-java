package br.com.fiap.contacerta.service.front;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class LimiteDeEnviosTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T12:00:00Z");

    @Test
    void recusaQuemPassaDoLimiteEDeixaOsOutros() {
        LimiteDeEnvios limite = new LimiteDeEnvios(2, Duration.ofMinutes(10));
        assertTrue(limite.permitir("1.1.1.1", AGORA));
        assertTrue(limite.permitir("1.1.1.1", AGORA.plusSeconds(1)));
        assertFalse(limite.permitir("1.1.1.1", AGORA.plusSeconds(2)));
        assertTrue(limite.permitir("2.2.2.2", AGORA.plusSeconds(2)));
    }

    @Test
    void depoisDaJanelaVoltaALiberar() {
        LimiteDeEnvios limite = new LimiteDeEnvios(1, Duration.ofMinutes(10));
        assertTrue(limite.permitir("1.1.1.1", AGORA));
        assertFalse(limite.permitir("1.1.1.1", AGORA.plusSeconds(60)));
        assertTrue(limite.permitir("1.1.1.1", AGORA.plus(Duration.ofMinutes(11))));
    }
}
