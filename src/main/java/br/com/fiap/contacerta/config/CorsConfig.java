package br.com.fiap.contacerta.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS: por segurança o navegador bloqueia um site (o front em React) de chamar uma API
 * que está em outro endereço, a não ser que a API diga "essa origem pode".
 * Aqui liberamos o front local (Vite usa a porta 5173) e o front publicado,
 * cuja URL vem da variável de ambiente FRONT_URL.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String FRONT_LOCAL = "http://localhost:5173";

    private final String frontUrl;

    public CorsConfig(@Value("${app.cors.front-url:}") String frontUrl) {
        this.frontUrl = frontUrl;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origensLiberadas().toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // O front precisa conseguir ler o header Location devolvido nos cadastros (201)
                .exposedHeaders("Location")
                .maxAge(3600);
    }

    /** Monta a lista de origens: sempre o front local e, se configurado, o front publicado. */
    List<String> origensLiberadas() {
        List<String> origens = new ArrayList<>();
        origens.add(FRONT_LOCAL);
        if (frontUrl != null && !frontUrl.isBlank()) {
            // Uma origem não tem barra no final; removemos para não dar diferença na comparação.
            String url = frontUrl.trim();
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            if (!origens.contains(url)) {
                origens.add(url);
            }
        }
        return origens;
    }
}
