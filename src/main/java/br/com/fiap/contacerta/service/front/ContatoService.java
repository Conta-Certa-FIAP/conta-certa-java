package br.com.fiap.contacerta.service.front;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.ContatoDAO;
import br.com.fiap.contacerta.dto.front.ContactMessageDto;
import br.com.fiap.contacerta.exception.MuitasRequisicoesException;
import br.com.fiap.contacerta.model.MensagemContato;

/** Guarda as mensagens do formulário "Contato" do site (POST /contact). */
@Service
public class ContatoService {

    /** Até 5 mensagens a cada 10 minutos por origem: sobra para uma pessoa, falta para um robô. */
    private final LimiteDeEnvios limite = new LimiteDeEnvios(5, Duration.ofMinutes(10));

    private final ContatoDAO contatoDAO;

    public ContatoService(ContatoDAO contatoDAO) {
        this.contatoDAO = contatoDAO;
    }

    /** @param origem endereço de quem enviou (IP), usado só para o limite de envios */
    public void registrar(ContactMessageDto mensagem, String origem) {
        if (!limite.permitir(origem, Instant.now())) {
            throw new MuitasRequisicoesException("Muitas mensagens em pouco tempo. Tente de novo daqui a alguns minutos.");
        }
        contatoDAO.inserir(new MensagemContato(
                mensagem.name().trim(),
                mensagem.email().trim().toLowerCase(),
                mensagem.message().trim(),
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
    }
}
