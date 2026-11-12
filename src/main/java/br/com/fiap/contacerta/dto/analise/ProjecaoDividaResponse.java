package br.com.fiap.contacerta.dto.analise;

import java.math.BigDecimal;
import java.util.List;

/**
 * Saída de GET /dividas/{id}/projecao: como a dívida evolui nos próximos meses em dois
 * cenários, "pagando a parcela" e "sem pagar nada".
 */
public record ProjecaoDividaResponse(
        Long dividaId,
        String credor,
        String tipoDescricao,
        BigDecimal saldoDevedor,
        BigDecimal taxaJurosMensal,
        BigDecimal valorParcela,
        int meses,
        List<MesProjecao> pagandoAParcela,
        List<MesProjecao> semPagarNada,
        BigDecimal totalJurosPagando,        // juros somados no cenário "pagando a parcela"
        BigDecimal saldoFinalPagando,
        BigDecimal saldoFinalSemPagar,
        BigDecimal crescimentoSemPagar,      // quanto a dívida aumenta se nada for pago
        EncargosAtraso encargosAtraso,       // nulo se a dívida não estiver atrasada
        List<String> avisos) {

    /** Um mês da projeção. */
    public record MesProjecao(int mes, BigDecimal saldoInicial, BigDecimal juros,
                              BigDecimal taxaJurosPercentual, BigDecimal parcela, BigDecimal saldoFinal) {
    }

    /** Multa e juros de mora da parcela vencida e quanto custa quitar tudo hoje. */
    public record EncargosAtraso(int diasAtraso, BigDecimal multa, BigDecimal jurosMora,
                                 BigDecimal parcelaAtualizada, BigDecimal valorParaQuitarHoje,
                                 String baseLegal) {
    }
}
