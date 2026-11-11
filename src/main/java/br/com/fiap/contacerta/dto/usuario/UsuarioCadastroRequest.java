package br.com.fiap.contacerta.dto.usuario;

import java.math.BigDecimal;

import br.com.fiap.contacerta.dto.validacao.CpfValido;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Entrada de POST /auth/cadastro. As anotações são conferidas pelo @Valid do controller. */
public class UsuarioCadastroRequest {

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
    private String email;

    @NotBlank(message = "O CPF é obrigatório")
    @CpfValido
    private String cpf;

    // A regex exige pelo menos uma letra e um número. O limite de 72 é do próprio BCrypt.
    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "A senha deve ter pelo menos uma letra e um número")
    private String senha;

    // Opcional
    @PositiveOrZero(message = "A renda não pode ser negativa")
    @Digits(integer = 10, fraction = 2, message = "A renda deve ter no máximo 10 dígitos e 2 casas decimais")
    private BigDecimal rendaDeclarada;

    // Opcional (se não vier, a API usa 220 horas)
    @Min(value = 1, message = "As horas de trabalho por mês devem ser no mínimo 1")
    @Max(value = 744, message = "As horas de trabalho por mês devem ser no máximo 744")
    private Integer horasTrabalhoMes;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public BigDecimal getRendaDeclarada() { return rendaDeclarada; }
    public void setRendaDeclarada(BigDecimal rendaDeclarada) { this.rendaDeclarada = rendaDeclarada; }

    public Integer getHorasTrabalhoMes() { return horasTrabalhoMes; }
    public void setHorasTrabalhoMes(Integer horasTrabalhoMes) { this.horasTrabalhoMes = horasTrabalhoMes; }
}
