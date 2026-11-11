package br.com.fiap.contacerta.dto.erro;

/** Um campo que falhou na validação e a mensagem para mostrar ao usuário. */
public record CampoErro(String campo, String mensagem) {
}
