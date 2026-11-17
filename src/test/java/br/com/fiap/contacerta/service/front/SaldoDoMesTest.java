package br.com.fiap.contacerta.service.front;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.fiap.contacerta.dto.front.DashboardDto.BalancePoint;
import br.com.fiap.contacerta.model.MeioPagamento;
import br.com.fiap.contacerta.model.TipoMovimento;
import br.com.fiap.contacerta.model.Transacao;

class SaldoDoMesTest {

    private static Transacao transacao(int dia, TipoMovimento tipo, MeioPagamento meio, String valor) {
        Transacao transacao = new Transacao();
        transacao.setData(LocalDate.of(2026, 11, dia));
        transacao.setTipo(tipo);
        transacao.setMeio(meio);
        transacao.setValor(new BigDecimal(valor));
        return transacao;
    }

    @Test
    void reconstroiOSaldoDeCadaDiaDesfazendoOQueVeioDepois() {
        LocalDate hoje = LocalDate.of(2026, 11, 4);
        List<Transacao> transacoes = List.of(
                transacao(2, TipoMovimento.ENTRADA, MeioPagamento.PIX, "1000.00"),
                transacao(3, TipoMovimento.SAIDA, MeioPagamento.PIX, "200.00"),
                transacao(3, TipoMovimento.SAIDA, MeioPagamento.CARTAO_CREDITO, "999.00")); // fatura: não mexe no saldo

        List<BalancePoint> pontos = SaldoDoMes.historico(YearMonth.of(2026, 11), hoje, new BigDecimal("900.00"), transacoes);

        assertEquals(4, pontos.size());
        assertEquals(new BigDecimal("100.00"), pontos.get(0).balance()); // dia 1: antes do salário
        assertEquals(new BigDecimal("1100.00"), pontos.get(1).balance()); // dia 2: salário entrou
        assertEquals(new BigDecimal("900.00"), pontos.get(2).balance()); // dia 3: saiu o PIX
        assertEquals(new BigDecimal("900.00"), pontos.get(3).balance()); // hoje
    }

    @Test
    void mesQueJaAcabouVaiAteOUltimoDia() {
        List<BalancePoint> pontos = SaldoDoMes.historico(YearMonth.of(2026, 10), LocalDate.of(2026, 11, 17),
                BigDecimal.TEN, List.of());
        assertEquals(31, pontos.size());
        assertEquals(LocalDate.of(2026, 10, 31), pontos.get(30).date());
    }

    @Test
    void previsaoSomaAMediaDiariaAteOFimDoMes() {
        LocalDate hoje = LocalDate.of(2026, 11, 27);
        List<Transacao> ultimos = List.of(
                transacao(1, TipoMovimento.ENTRADA, MeioPagamento.PIX, "300.00"),
                transacao(5, TipoMovimento.SAIDA, MeioPagamento.PIX, "90.00")); // líquido +210 em 30 dias = +7/dia

        List<BalancePoint> pontos = SaldoDoMes.previsao(hoje, new BigDecimal("100.00"), ultimos, 30);

        assertEquals(3, pontos.size()); // 28, 29 e 30 de novembro
        assertEquals(new BigDecimal("107.00"), pontos.get(0).balance());
        assertEquals(new BigDecimal("121.00"), pontos.get(2).balance());
    }

    @Test
    void ultimoDiaDoMesNaoTemPrevisao() {
        assertEquals(0, SaldoDoMes.previsao(LocalDate.of(2026, 11, 30), BigDecimal.ONE, List.of(), 1).size());
    }
}
