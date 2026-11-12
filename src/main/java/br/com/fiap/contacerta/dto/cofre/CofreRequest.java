package br.com.fiap.contacerta.dto.cofre;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Entrada de POST /usuarios/{id}/cofres e PUT /cofres/{id}. Meta e prazo são opcionais. */
public class CofreRequest {

    @NotBlank(message = "O nome do cofre é obrigatório")
    @Size(max = 60, message = "O nome deve ter no máximo 60 caracteres")
    private String nome;

    /** Opcional: quanto a pessoa quer juntar. */
    @Positive(message = "A meta deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "A meta deve ter no máximo 10 dígitos e 2 casas decimais")
    private BigDecimal valorMeta;

    /** Opcional: até quando quer bater a meta (AAAA-MM-DD). */
    @Future(message = "A data limite deve ser no futuro")
    private LocalDate dataLimite;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public BigDecimal getValorMeta() { return valorMeta; }
    public void setValorMeta(BigDecimal valorMeta) { this.valorMeta = valorMeta; }

    public LocalDate getDataLimite() { return dataLimite; }
    public void setDataLimite(LocalDate dataLimite) { this.dataLimite = dataLimite; }
}
