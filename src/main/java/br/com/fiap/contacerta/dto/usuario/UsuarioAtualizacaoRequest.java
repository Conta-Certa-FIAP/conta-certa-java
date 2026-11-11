package br.com.fiap.contacerta.dto.usuario;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Entrada de PUT /usuarios/{id}. CPF e senha não são alterados por esta rota. */
public class UsuarioAtualizacaoRequest {

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
    private String email;

    @PositiveOrZero(message = "A renda não pode ser negativa")
    @Digits(integer = 10, fraction = 2, message = "A renda deve ter no máximo 10 dígitos e 2 casas decimais")
    private BigDecimal rendaDeclarada;

    @Min(value = 1, message = "As horas de trabalho por mês devem ser no mínimo 1")
    @Max(value = 744, message = "As horas de trabalho por mês devem ser no máximo 744")
    private Integer horasTrabalhoMes;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public BigDecimal getRendaDeclarada() { return rendaDeclarada; }
    public void setRendaDeclarada(BigDecimal rendaDeclarada) { this.rendaDeclarada = rendaDeclarada; }

    public Integer getHorasTrabalhoMes() { return horasTrabalhoMes; }
    public void setHorasTrabalhoMes(Integer horasTrabalhoMes) { this.horasTrabalhoMes = horasTrabalhoMes; }
}
