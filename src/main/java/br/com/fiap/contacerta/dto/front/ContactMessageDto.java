package br.com.fiap.contacerta.dto.front;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Entrada de POST /contact: o formulário "Contato" do site público. */
public record ContactMessageDto(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 80, message = "O nome deve ter entre 2 e 80 caracteres")
        String name,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "A mensagem é obrigatória")
        @Size(min = 10, max = 1000, message = "A mensagem deve ter entre 10 e 1000 caracteres")
        String message) {
}
