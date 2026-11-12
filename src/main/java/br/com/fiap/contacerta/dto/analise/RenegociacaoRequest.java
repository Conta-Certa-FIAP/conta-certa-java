package br.com.fiap.contacerta.dto.analise;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Entrada de POST /dividas/{id}/renegociacao/simular: a proposta que o credor ofereceu. */
public class RenegociacaoRequest {

    /** Desconto para pagar tudo à vista, em %. Ex.: 40 = 40% de desconto. */
    @NotNull(message = "Informe o percentual de desconto à vista (use 0 se não houver)")
    @DecimalMin(value = "0", message = "O desconto não pode ser negativo")
    @DecimalMax(value = "100", message = "O desconto deve ser de no máximo 100%")
    private BigDecimal percentualDescontoAVista;

    @NotNull(message = "Informe a nova quantidade de parcelas")
    @Min(value = 1, message = "A nova quantidade de parcelas deve ser no mínimo 1")
    @Max(value = 600, message = "A nova quantidade de parcelas deve ser no máximo 600")
    private Integer novaQtParcelas;

    /** Nova taxa de juros, em % ao mês. */
    @NotNull(message = "Informe a nova taxa mensal (use 0 se não houver juros)")
    @DecimalMin(value = "0", message = "A nova taxa não pode ser negativa")
    @DecimalMax(value = "100", message = "A nova taxa deve ser de no máximo 100% ao mês")
    private BigDecimal novaTaxaMensal;

    public BigDecimal getPercentualDescontoAVista() { return percentualDescontoAVista; }
    public void setPercentualDescontoAVista(BigDecimal percentualDescontoAVista) { this.percentualDescontoAVista = percentualDescontoAVista; }

    public Integer getNovaQtParcelas() { return novaQtParcelas; }
    public void setNovaQtParcelas(Integer novaQtParcelas) { this.novaQtParcelas = novaQtParcelas; }

    public BigDecimal getNovaTaxaMensal() { return novaTaxaMensal; }
    public void setNovaTaxaMensal(BigDecimal novaTaxaMensal) { this.novaTaxaMensal = novaTaxaMensal; }
}
