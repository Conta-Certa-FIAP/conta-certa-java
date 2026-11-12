package br.com.fiap.contacerta.dto.divida;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Entrada de POST /usuarios/{id}/dividas e PUT /dividas/{id}.
 * O status (EM_DIA, ATRASADA, QUITADA) NÃO é enviado: a API calcula a partir do saldo
 * devedor e dos dias de atraso, para os dois nunca ficarem incoerentes.
 */
public class DividaRequest {

    /** Opcional: conta bancária ligada à dívida (precisa ser do mesmo usuário). */
    @Positive(message = "Conta inválida")
    private Long contaId;

    @NotBlank(message = "O credor é obrigatório")
    @Size(max = 80, message = "O credor deve ter no máximo 80 caracteres")
    private String credor;

    @NotBlank(message = "O tipo é obrigatório")
    @Pattern(regexp = "CARTAO_ROTATIVO|CARTAO_PARCELADO|CHEQUE_ESPECIAL|EMPRESTIMO_PESSOAL|CONSIGNADO"
            + "|FINANCIAMENTO_VEICULO|FINANCIAMENTO_IMOVEL|CREDIARIO|CONTA_ATRASADA|OUTRO",
            message = "Tipo inválido. Use: CARTAO_ROTATIVO, CARTAO_PARCELADO, CHEQUE_ESPECIAL, EMPRESTIMO_PESSOAL,"
                    + " CONSIGNADO, FINANCIAMENTO_VEICULO, FINANCIAMENTO_IMOVEL, CREDIARIO, CONTA_ATRASADA ou OUTRO")
    private String tipo;

    @NotNull(message = "O valor original é obrigatório")
    @Positive(message = "O valor original deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "O valor original deve ter no máximo 10 dígitos e 2 casas decimais")
    private BigDecimal valorOriginal;

    /** Opcional: quanto falta pagar hoje. Se não vier, a API assume o valor original. */
    @PositiveOrZero(message = "O saldo devedor não pode ser negativo")
    @Digits(integer = 10, fraction = 2, message = "O saldo devedor deve ter no máximo 10 dígitos e 2 casas decimais")
    private BigDecimal saldoDevedor;

    /** Em % ao mês. Ex.: 13.5 = 13,5% ao mês. */
    @NotNull(message = "A taxa de juros mensal é obrigatória (use 0 se não houver juros)")
    @DecimalMin(value = "0", message = "A taxa de juros não pode ser negativa")
    @DecimalMax(value = "100", message = "A taxa de juros deve ser de no máximo 100% ao mês")
    @Digits(integer = 3, fraction = 3, message = "A taxa de juros deve ter no máximo 3 casas decimais")
    private BigDecimal taxaJurosMensal;

    @NotNull(message = "A quantidade de parcelas é obrigatória (use 1 para dívida sem parcelamento)")
    @Min(value = 1, message = "A quantidade de parcelas deve ser no mínimo 1")
    @Max(value = 9999, message = "A quantidade de parcelas deve ser no máximo 9999")
    private Integer parcelasTotal;

    /** Opcional (padrão 0). */
    @Min(value = 0, message = "As parcelas pagas não podem ser negativas")
    private Integer parcelasPagas;

    /** Opcional: se não vier, a API calcula pela Tabela Price. */
    @Positive(message = "O valor da parcela deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "A parcela deve ter no máximo 10 dígitos e 2 casas decimais")
    private BigDecimal valorParcela;

    @NotNull(message = "A data de contratação é obrigatória")
    @PastOrPresent(message = "A data de contratação não pode ser no futuro")
    private LocalDate dataContratacao;

    /** Opcional. Se já passou e "diasAtraso" não vier, a API calcula o atraso por esta data. */
    private LocalDate dataProximoVencimento;

    /** Opcional. */
    @Min(value = 0, message = "Os dias de atraso não podem ser negativos")
    @Max(value = 99999, message = "Os dias de atraso devem ser no máximo 99999")
    private Integer diasAtraso;

    public Long getContaId() { return contaId; }
    public void setContaId(Long contaId) { this.contaId = contaId; }

    public String getCredor() { return credor; }
    public void setCredor(String credor) { this.credor = credor; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public BigDecimal getValorOriginal() { return valorOriginal; }
    public void setValorOriginal(BigDecimal valorOriginal) { this.valorOriginal = valorOriginal; }

    public BigDecimal getSaldoDevedor() { return saldoDevedor; }
    public void setSaldoDevedor(BigDecimal saldoDevedor) { this.saldoDevedor = saldoDevedor; }

    public BigDecimal getTaxaJurosMensal() { return taxaJurosMensal; }
    public void setTaxaJurosMensal(BigDecimal taxaJurosMensal) { this.taxaJurosMensal = taxaJurosMensal; }

    public Integer getParcelasTotal() { return parcelasTotal; }
    public void setParcelasTotal(Integer parcelasTotal) { this.parcelasTotal = parcelasTotal; }

    public Integer getParcelasPagas() { return parcelasPagas; }
    public void setParcelasPagas(Integer parcelasPagas) { this.parcelasPagas = parcelasPagas; }

    public BigDecimal getValorParcela() { return valorParcela; }
    public void setValorParcela(BigDecimal valorParcela) { this.valorParcela = valorParcela; }

    public LocalDate getDataContratacao() { return dataContratacao; }
    public void setDataContratacao(LocalDate dataContratacao) { this.dataContratacao = dataContratacao; }

    public LocalDate getDataProximoVencimento() { return dataProximoVencimento; }
    public void setDataProximoVencimento(LocalDate dataProximoVencimento) { this.dataProximoVencimento = dataProximoVencimento; }

    public Integer getDiasAtraso() { return diasAtraso; }
    public void setDiasAtraso(Integer diasAtraso) { this.diasAtraso = diasAtraso; }
}
