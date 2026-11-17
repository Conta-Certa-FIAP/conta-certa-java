package br.com.fiap.contacerta.dto.front;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Saída de GET /dashboard?month=AAAA-MM: tudo o que a tela "Início" do front mostra.
 *
 * @param forecastBalance saldo previsto para o fim do mês (nulo fora do mês atual)
 * @param balanceHistory  saldo real, do dia 1 até hoje
 * @param balanceForecast saldo previsto, de amanhã até o fim do mês
 * @param interestAlert   aviso "Juros acima da média" (nulo se nenhuma dívida passar da média)
 * @param plan            nulo quando não há dívida ativa
 * @param learning        nulo quando não há trilha
 */
public record DashboardDto(
        String userName,
        BigDecimal balance,
        BigDecimal monthIncome,
        BigDecimal monthExpense,
        BigDecimal forecastBalance,
        List<BalancePoint> balanceHistory,
        List<BalancePoint> balanceForecast,
        List<CategoryExpense> expensesByCategory,
        List<UpcomingPayment> upcomingPayments,
        InterestAlert interestAlert,
        Debts debts,
        Plan plan,
        Learning learning) {

    /** Um ponto do gráfico "Saldo ao longo do mês". */
    public record BalancePoint(LocalDate date, BigDecimal balance) {
    }

    public record CategoryExpense(String category, BigDecimal total) {
    }

    public record UpcomingPayment(Long id, Long debtId, String title, String description,
                                  LocalDate dueDate, BigDecimal amount) {
    }

    public record InterestAlert(Long debtId, String debtName, BigDecimal monthlyRate, BigDecimal marketRate) {
    }

    /** Cartão "Minhas dívidas": quantidade, soma dos saldos e quantas estão com juros altos. */
    public record Debts(int count, BigDecimal total, int alerts) {
    }

    /** Cartão "Meu plano". debtFreeDate em "AAAA-MM". */
    public record Plan(String debtFreeDate, int progressPercent) {
    }

    public record Learning(String trackTitle, int completedLessons, int totalLessons) {
    }
}
