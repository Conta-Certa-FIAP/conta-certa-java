package br.com.fiap.contacerta.dto.front;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma dívida no formato que o front espera (item de GET /debts).
 *
 * Os DTOs deste pacote têm os campos em INGLÊS de propósito: são exatamente os nomes do
 * contrato escrito pelo time do front (arquivo ENDPOINTS.md e pasta src/types do front).
 *
 * @param type              texto pronto do tipo de crédito, ex.: "Rotativo do cartão"
 * @param monthlyRate       taxa da dívida, em % ao mês
 * @param marketRate        média do mercado para esse tipo de crédito, em % ao mês
 * @param installmentsPaid  nulo quando a dívida não é parcelada
 * @param installmentsTotal nulo quando a dívida não é parcelada
 * @param status            EM_DIA, ATENCAO ou JUROS_ALTOS
 */
public record DebtDto(
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
        String status) {
}
