package br.com.fiap.contacerta.dto.analise;

import java.math.BigDecimal;

/**
 * Saída de POST /dividas/{id}/renegociacao/simular: compara três cenários.
 * É só simulação: nada é alterado na dívida.
 */
public record RenegociacaoResponse(
        Long dividaId,
        String credor,
        BigDecimal saldoDevedor,
        Cenario continuarComoEsta,
        Cenario aVistaComDesconto,
        Cenario novoParcelamento,
        String melhorCenario,     // CONTINUAR, A_VISTA ou NOVO_PARCELAMENTO
        BigDecimal economia,      // quanto o melhor cenário economiza em relação a continuar como está
        String mensagem) {

    /** Um cenário: quanto se paga no total, em quantas parcelas e de quanto. */
    public record Cenario(
            String codigo,
            String nome,
            boolean quitavel,         // false = a parcela atual não cobre os juros (nunca quita)
            BigDecimal totalAPagar,
            Integer parcelas,
            BigDecimal valorParcela,
            BigDecimal totalJuros,
            String observacao) {
    }
}
