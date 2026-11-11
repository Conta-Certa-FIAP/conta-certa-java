package br.com.fiap.contacerta.dto.validacao;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Anotação PRÓPRIA do projeto para validar CPF (em vez da @CPF do pacote org.hibernate.*).
 * Uso: colocar @CpfValido em cima de um campo String do DTO.
 * Quem faz a conta dos dígitos é a classe CpfValidador.
 */
@Documented
@Constraint(validatedBy = CpfValidador.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CpfValido {

    String message() default "CPF inválido";

    // Os dois atributos abaixo são exigidos pelo Bean Validation em toda anotação de validação.
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
