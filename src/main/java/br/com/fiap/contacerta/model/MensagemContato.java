package br.com.fiap.contacerta.model;

import java.time.LocalDateTime;

/**
 * Espelha a tabela tb_contato: uma mensagem enviada pelo formulário "Contato" do site.
 *
 * @param nome      nm_remetente
 * @param email     ds_email
 * @param mensagem  ds_mensagem
 * @param dataEnvio dt_envio
 */
public record MensagemContato(String nome, String email, String mensagem, LocalDateTime dataEnvio) {
}
