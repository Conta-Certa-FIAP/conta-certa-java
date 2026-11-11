package br.com.fiap.contacerta.dto.validacao;

import br.com.fiap.contacerta.service.CpfUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Classe que o Bean Validation chama quando encontra @CpfValido.
 * Campo vazio/nulo é considerado válido AQUI porque quem cobra "obrigatório" é o @NotBlank;
 * assim cada anotação tem uma única responsabilidade e a mensagem de erro fica certa.
 */
public class CpfValidador implements ConstraintValidator<CpfValido, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null || valor.isBlank()) {
            return true;
        }
        return CpfUtil.valido(valor);
    }
}
