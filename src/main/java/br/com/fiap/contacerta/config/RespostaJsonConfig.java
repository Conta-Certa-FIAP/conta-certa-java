package br.com.fiap.contacerta.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Garante que a API SEMPRE responde em JSON, inclusive nos erros.
 * Sem isso, um cliente que peça só HTML (header "Accept: text/html") receberia um erro
 * sem corpo. Ignorando o header Accept, a resposta é JSON para qualquer cliente.
 */
@Configuration
public class RespostaJsonConfig implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.ignoreAcceptHeader(true).defaultContentType(MediaType.APPLICATION_JSON);
    }
}
