package br.com.fiap.contacerta.seguranca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import br.com.fiap.contacerta.exception.NaoAutenticadoException;

class TokenServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T12:00:00Z");

    private final TokenService service = new TokenService("segredo-de-teste-com-mais-de-trinta-caracteres");

    @Test
    void tokenValidoDevolveOIdDoUsuario() {
        String token = service.gerar(42L, AGORA);
        assertEquals(42L, service.validar(token, AGORA.plusSeconds(60)));
    }

    @Test
    void tokenVencidoERecusado() {
        String token = service.gerar(42L, AGORA);
        Instant depoisDaValidade = AGORA.plus(TokenService.VALIDADE).plusSeconds(1);
        assertThrows(NaoAutenticadoException.class, () -> service.validar(token, depoisDaValidade));
    }

    @Test
    void trocarOIdDentroDoTokenQuebraAAssinatura() {
        String token = service.gerar(42L, AGORA);
        String assinatura = token.split("\\.")[1];
        String dadosFalsos = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("7." + AGORA.plus(TokenService.VALIDADE).getEpochSecond()).getBytes(StandardCharsets.UTF_8));
        assertThrows(NaoAutenticadoException.class, () -> service.validar(dadosFalsos + "." + assinatura, AGORA));
    }

    @Test
    void tokenDeOutroSegredoERecusado() {
        String token = new TokenService("outro-segredo-qualquer-com-muitos-caracteres").gerar(42L, AGORA);
        assertThrows(NaoAutenticadoException.class, () -> service.validar(token, AGORA));
    }

    @Test
    void segredoCurtoImpedeAApiDeSubir() {
        assertThrows(IllegalStateException.class, () -> new TokenService("123"));
    }

    @Test
    void semSegredoNoRenderAApiNaoSobe() {
        assertThrows(IllegalStateException.class, () -> new TokenService("", true));
    }

    @Test
    void tokenComPartesSobrandoERecusado() {
        String token = service.gerar(42L, AGORA);
        assertThrows(NaoAutenticadoException.class, () -> service.validar(token + ".", AGORA));
    }

    @Test
    void lixoNoLugarDoTokenViraNaoAutenticado() {
        assertThrows(NaoAutenticadoException.class, () -> service.validar("abc", AGORA));
        assertThrows(NaoAutenticadoException.class, () -> service.validar("@@@.###", AGORA));
        assertThrows(NaoAutenticadoException.class, () -> service.validar("", AGORA));
    }
}
