package br.com.fiap.contacerta.service.front;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import br.com.fiap.contacerta.dto.front.DashboardDto.BalancePoint;
import br.com.fiap.contacerta.model.MeioPagamento;
import br.com.fiap.contacerta.model.TipoMovimento;
import br.com.fiap.contacerta.model.Transacao;

/**
 * Cálculos do gráfico "Saldo ao longo do mês" da tela Início.
 * Só métodos estáticos e sem banco de dados (testados em SaldoDoMesTest).
 *
 * Como reconstruir o saldo de um dia que já passou: partimos do saldo de HOJE e "desfazemos"
 * as transações que aconteceram depois daquele dia. Por exemplo, se hoje há R$ 500 e ontem à
 * noite entrou um salário de R$ 2.000 que ainda não tinha caído no dia anterior, o saldo do dia
 * anterior era R$ 500 - R$ 2.000 = -R$ 1.500.
 */
public final class SaldoDoMes {

    /** A previsão olha os últimos 90 dias de movimentação. */
    static final int DIAS_DA_MEDIA = 90;

    /** Com menos de 30 dias de movimentação a média não representa o mês: não fazemos previsão. */
    static final int DIAS_MINIMOS_PARA_PREVER = 30;

    private SaldoDoMes() {
    }

    /**
     * Quanto a transação mexeu no saldo da conta: entrada soma, saída tira.
     * Compra no cartão de CRÉDITO não mexe no saldo (vai para a fatura), então conta zero.
     */
    static BigDecimal efeito(Transacao transacao) {
        if (transacao.getTipo() == TipoMovimento.ENTRADA) {
            return transacao.getValor();
        }
        return transacao.getMeio() == MeioPagamento.CARTAO_CREDITO ? BigDecimal.ZERO : transacao.getValor().negate();
    }

    /**
     * Saldo no fim de cada dia do mês, do dia 1 até hoje (ou até o último dia, se o mês já acabou).
     *
     * @param saldoHoje  saldo atual somando todas as contas
     * @param transacoes todas as transações do dia 1 do mês até hoje
     */
    public static List<BalancePoint> historico(YearMonth mes, LocalDate hoje, BigDecimal saldoHoje,
                                               List<Transacao> transacoes) {
        List<BalancePoint> pontos = new ArrayList<>();
        LocalDate ultimoDia = mes.atEndOfMonth().isBefore(hoje) ? mes.atEndOfMonth() : hoje;

        for (LocalDate dia = mes.atDay(1); !dia.isAfter(ultimoDia); dia = dia.plusDays(1)) {
            BigDecimal depois = BigDecimal.ZERO;
            for (Transacao transacao : transacoes) {
                if (transacao.getData().isAfter(dia)) {
                    depois = depois.add(efeito(transacao));
                }
            }
            pontos.add(new BalancePoint(dia, saldoHoje.subtract(depois)));
        }
        return pontos;
    }

    /**
     * Previsão simples, de amanhã até o fim do mês: o saldo de hoje mais a variação média por dia
     * dos últimos 90 dias. Não é um modelo de IA, é uma média: fácil de explicar e de conferir.
     *
     * @param ultimos90Dias transações dos últimos 90 dias (até hoje)
     * @param diasDeHistorico por quantos dias dividir (menos de 90 se a pessoa começou há pouco)
     */
    public static List<BalancePoint> previsao(LocalDate hoje, BigDecimal saldoHoje,
                                              List<Transacao> ultimos90Dias, int diasDeHistorico) {
        BigDecimal soma = ultimos90Dias.stream().map(SaldoDoMes::efeito).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal porDia = soma.divide(BigDecimal.valueOf(Math.max(1, diasDeHistorico)), 2, RoundingMode.HALF_UP);

        List<BalancePoint> pontos = new ArrayList<>();
        LocalDate fimDoMes = YearMonth.from(hoje).atEndOfMonth();
        BigDecimal saldo = saldoHoje;
        for (LocalDate dia = hoje.plusDays(1); !dia.isAfter(fimDoMes); dia = dia.plusDays(1)) {
            saldo = saldo.add(porDia);
            pontos.add(new BalancePoint(dia, saldo));
        }
        return pontos;
    }
}
