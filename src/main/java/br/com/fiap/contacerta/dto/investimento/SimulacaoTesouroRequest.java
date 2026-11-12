package br.com.fiap.contacerta.dto.investimento;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Entrada de POST /investimentos/tesouro-selic/simular. */
public class SimulacaoTesouroRequest {

    @NotNull(message = "O valor inicial é obrigatório (use 0 se não houver)")
    @PositiveOrZero(message = "O valor inicial não pode ser negativo")
    @Digits(integer = 10, fraction = 2, message = "O valor inicial deve ter no máximo 10 dígitos e 2 casas decimais")
    private BigDecimal valorInicial;

    @NotNull(message = "O aporte mensal é obrigatório (use 0 se não houver)")
    @PositiveOrZero(message = "O aporte mensal não pode ser negativo")
    @Digits(integer = 10, fraction = 2, message = "O aporte mensal deve ter no máximo 10 dígitos e 2 casas decimais")
    private BigDecimal aporteMensal;

    @NotNull(message = "A quantidade de meses é obrigatória")
    @Min(value = 1, message = "A simulação deve ter no mínimo 1 mês")
    @Max(value = 600, message = "A simulação deve ter no máximo 600 meses")
    private Integer meses;

    /** Opcional, em % ao ano. Se não vier, a API usa a Selic atual do Banco Central. */
    @DecimalMin(value = "0", message = "A Selic não pode ser negativa")
    @DecimalMax(value = "100", message = "A Selic deve ser de no máximo 100% ao ano")
    private BigDecimal selicAnual;

    /** Opcional, em % ao ano. Se não vier, a API usa o IPCA acumulado em 12 meses do Banco Central. */
    @DecimalMin(value = "-50", message = "O IPCA deve ser de no mínimo -50% ao ano")
    @DecimalMax(value = "100", message = "O IPCA deve ser de no máximo 100% ao ano")
    private BigDecimal ipcaAnual;

    public BigDecimal getValorInicial() { return valorInicial; }
    public void setValorInicial(BigDecimal valorInicial) { this.valorInicial = valorInicial; }

    public BigDecimal getAporteMensal() { return aporteMensal; }
    public void setAporteMensal(BigDecimal aporteMensal) { this.aporteMensal = aporteMensal; }

    public Integer getMeses() { return meses; }
    public void setMeses(Integer meses) { this.meses = meses; }

    public BigDecimal getSelicAnual() { return selicAnual; }
    public void setSelicAnual(BigDecimal selicAnual) { this.selicAnual = selicAnual; }

    public BigDecimal getIpcaAnual() { return ipcaAnual; }
    public void setIpcaAnual(BigDecimal ipcaAnual) { this.ipcaAnual = ipcaAnual; }
}
