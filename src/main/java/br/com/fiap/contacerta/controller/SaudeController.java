package br.com.fiap.contacerta.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health - "a API está de pé?".
 * O Render chama esta rota para saber se o deploy deu certo. Ela não consulta o banco de
 * propósito: assim a resposta é imediata e não gasta uma conexão do Oracle a cada checagem.
 */
@RestController
public class SaudeController {

    @GetMapping("/health")
    public Map<String, String> saude() {
        return Map.of("status", "ok", "api", "conta-certa-api");
    }
}
