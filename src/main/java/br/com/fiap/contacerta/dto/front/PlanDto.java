package br.com.fiap.contacerta.dto.front;

import java.math.BigDecimal;
import java.util.List;

/**
 * Saída de GET /plan e PUT /plan. Os meses vão como texto "AAAA-MM".
 *
 * @param debtFreeDate mês em que a pessoa fica livre das dívidas; nulo se, com o valor mensal
 *                     informado, as dívidas não são quitadas (ver "warning")
 * @param order        ordem de pagamento (vazia quando não há dívida ativa)
 * @param options      a simulação de cada método, para comparar
 * @param warning      aviso da simulação (ex.: valor mensal não cobre os juros); nulo se estiver tudo certo
 */
public record PlanDto(
        BigDecimal monthlyBudget,
        String method,
        String debtFreeDate,
        int progressPercent,
        BigDecimal paidAmount,
        BigDecimal totalAmount,
        List<Step> order,
        List<Option> options,
        String warning) {

    /** Uma dívida na ordem de pagamento. payoffDate é nulo se ela não é quitada no plano. */
    public record Step(int position, Long debtId, String debtName, String payoffDate,
                       BigDecimal balance, int progressPercent) {
    }

    public record Option(String method, String debtFreeDate, BigDecimal totalInterest, boolean recommended) {
    }
}
