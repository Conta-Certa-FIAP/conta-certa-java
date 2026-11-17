package br.com.fiap.contacerta.seguranca;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.fiap.contacerta.exception.NaoAutenticadoException;

/**
 * Cria e confere o token que o login devolve.
 *
 * O front guarda o token e o envia em toda requisição no cabeçalho
 * "Authorization: Bearer <token>". É por ele que a API sabe QUEM está chamando,
 * sem precisar receber o id do usuário de novo.
 *
 * Formato do token (duas partes separadas por ponto, em Base64):
 *     dados . assinatura
 *   dados      = "idDoUsuario.validade"  (validade = instante em segundos)
 *   assinatura = HMAC-SHA256(dados, segredo)
 *
 * A assinatura é uma conta que só dá o mesmo resultado para quem conhece o segredo.
 * Se alguém trocar o id dentro do token, a assinatura deixa de bater e o token é recusado.
 * O segredo vem da variável de ambiente TOKEN_SECRET (nunca fica no repositório).
 */
@Component
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    static final Duration VALIDADE = Duration.ofHours(12);
    private static final String ALGORITMO = "HmacSHA256";
    private static final String MENSAGEM_INVALIDO = "Sessão inválida ou expirada. Entre de novo.";

    /** Segredo curto pode ser descoberto por tentativa e erro; 32 caracteres é o mínimo aceito. */
    static final int TAMANHO_MINIMO_DO_SEGREDO = 32;

    private final byte[] segredo;

    /** Usado pelos testes e quando a API roda no computador. */
    public TokenService(String segredo) {
        this(segredo, false);
    }

    /**
     * @param segredo     valor de TOKEN_SECRET
     * @param emProducao  true quando a API roda no Render (o Render define a variável RENDER=true)
     */
    @Autowired
    public TokenService(@Value("${app.token.segredo:}") String segredo,
                        @Value("${RENDER:false}") boolean emProducao) {
        if (segredo != null && !segredo.isBlank() && segredo.length() < TAMANHO_MINIMO_DO_SEGREDO) {
            throw new IllegalStateException(
                    "TOKEN_SECRET muito curto: use pelo menos " + TAMANHO_MINIMO_DO_SEGREDO + " caracteres.");
        }
        if ((segredo == null || segredo.isBlank()) && emProducao) {
            // Sem segredo fixo, todo reinício do servidor derrubaria o login de todo mundo.
            throw new IllegalStateException("Defina a variável de ambiente TOKEN_SECRET no Render.");
        }
        if (segredo == null || segredo.isBlank()) {
            // Sem TOKEN_SECRET (rodando no computador): sorteia um segredo que vale até a API reiniciar.
            byte[] aleatorio = new byte[32];
            new SecureRandom().nextBytes(aleatorio);
            this.segredo = aleatorio;
            log.warn("TOKEN_SECRET não definido: usando um segredo temporário. "
                    + "Os logins deixam de valer quando a API reiniciar.");
        } else {
            this.segredo = segredo.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String gerar(Long usuarioId) {
        return gerar(usuarioId, Instant.now());
    }

    /** Recebe o "agora" por parâmetro para os testes conseguirem simular um token vencido. */
    String gerar(Long usuarioId, Instant agora) {
        String dados = usuarioId + "." + agora.plus(VALIDADE).getEpochSecond();
        return codificar(dados.getBytes(StandardCharsets.UTF_8)) + "." + codificar(assinar(dados));
    }

    /** Devolve o id do usuário dono do token. Qualquer problema vira 401. */
    public Long validar(String token) {
        return validar(token, Instant.now());
    }

    Long validar(String token, Instant agora) {
        try {
            // O -1 mantém partes vazias: "a.b." vira 3 partes e é recusado.
            String[] partes = token.split("\\.", -1);
            if (partes.length != 2) {
                throw new NaoAutenticadoException(MENSAGEM_INVALIDO);
            }
            String dados = new String(Base64.getUrlDecoder().decode(partes[0]), StandardCharsets.UTF_8);
            byte[] assinaturaRecebida = Base64.getUrlDecoder().decode(partes[1]);

            // MessageDigest.isEqual compara em tempo constante (não dá pistas pelo tempo de resposta).
            if (!MessageDigest.isEqual(assinar(dados), assinaturaRecebida)) {
                throw new NaoAutenticadoException(MENSAGEM_INVALIDO);
            }

            String[] campos = dados.split("\\.", -1);
            if (campos.length != 2) {
                throw new NaoAutenticadoException(MENSAGEM_INVALIDO);
            }
            long usuarioId = Long.parseLong(campos[0]);
            long validade = Long.parseLong(campos[1]);
            if (agora.getEpochSecond() >= validade) {
                throw new NaoAutenticadoException(MENSAGEM_INVALIDO);
            }
            return usuarioId;
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            // Base64 quebrado, número que não é número, campo faltando...
            throw new NaoAutenticadoException(MENSAGEM_INVALIDO);
        }
    }

    private byte[] assinar(String dados) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(segredo, ALGORITMO));
            return mac.doFinal(dados.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível assinar o token", e);
        }
    }

    private static String codificar(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
