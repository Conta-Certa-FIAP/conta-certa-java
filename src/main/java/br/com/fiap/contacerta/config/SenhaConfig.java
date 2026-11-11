package br.com.fiap.contacerta.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt: transforma a senha num "hash" que não dá para desfazer.
 * No banco fica só o hash (coluna ds_senha_hash). No login, o BCrypt refaz a conta com a
 * senha digitada e compara. Cada hash tem um "sal" aleatório embutido, então duas pessoas
 * com a mesma senha ficam com hashes diferentes.
 *
 * Usamos apenas a biblioteca spring-security-crypto (sem o Spring Security completo).
 */
@Configuration
public class SenhaConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
