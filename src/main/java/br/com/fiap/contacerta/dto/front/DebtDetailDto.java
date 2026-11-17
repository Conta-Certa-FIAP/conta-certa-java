package br.com.fiap.contacerta.dto.front;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Saída de GET /debts/{id}: todos os campos de {@link DebtDto} mais o histórico e a trilha sugerida.
 * (Record não tem herança, por isso os campos da dívida aparecem repetidos aqui.)
 *
 * @param history      mais recente primeiro
 * @param relatedTrack trilha de aprendizado sugerida para esse tipo de dívida (pode ser nula)
 */
public record DebtDetailDto(
        Long id,
        String name,
        String type,
        String creditor,
        BigDecimal balance,
        BigDecimal monthlyRate,
        BigDecimal marketRate,
        Integer installmentsPaid,
        Integer installmentsTotal,
        LocalDate nextDueDate,
        BigDecimal minimumPayment,
        String status,
        List<DebtEvent> history,
        RelatedTrack relatedTrack) {

    /** Um item do histórico. "type" é PAGAMENTO ou CADASTRO. */
    public record DebtEvent(Long id, String type, LocalDate date, BigDecimal amount) {
    }

    public record RelatedTrack(Long id, String title) {
    }

    /** Junta a dívida já convertida com o histórico e a trilha. */
    public static DebtDetailDto de(DebtDto divida, List<DebtEvent> history, RelatedTrack relatedTrack) {
        return new DebtDetailDto(divida.id(), divida.name(), divida.type(), divida.creditor(), divida.balance(),
                divida.monthlyRate(), divida.marketRate(), divida.installmentsPaid(), divida.installmentsTotal(),
                divida.nextDueDate(), divida.minimumPayment(), divida.status(), history, relatedTrack);
    }
}
