package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fórmulas financeiras usadas em vários lugares (dívidas, renegociação, dados simulados).
 * Só métodos estáticos e sem banco: fáceis de testar (ver CalculadoraFinanceiraTest).
 *
 * Convenção do projeto: taxas chegam em PERCENTUAL (ex.: 2.5 significa 2,5% ao mês)
 * e os valores em dinheiro são BigDecimal com 2 casas (double perde centavos em somas).
 */
public final class CalculadoraFinanceira {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private CalculadoraFinanceira() {
    }

    /** Arredonda para centavos (2 casas, meio para cima). */
    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    /** 2.5 (%) -> 0.025 */
    public static double fracao(BigDecimal taxaPercentual) {
        return taxaPercentual.doubleValue() / 100.0;
    }

    /**
     * Parcela fixa pela Tabela Price:  PMT = PV * i / (1 - (1 + i)^-n)
     *   PV = valor financiado, i = taxa mensal, n = número de parcelas.
     * Se a taxa for zero não há juros: PMT = PV / n.
     */
    public static BigDecimal parcelaPrice(BigDecimal valor, BigDecimal taxaMensalPercentual, int parcelas) {
        double pv = valor.doubleValue();
        double i = fracao(taxaMensalPercentual);
        if (i == 0) {
            return arredondar(pv / parcelas);
        }
        double pmt = pv * i / (1 - Math.pow(1 + i, -parcelas));
        return arredondar(pmt);
    }

    /**
     * Caminho inverso da Price: quanto dá para financiar pagando essa parcela?
     *   PV = PMT * (1 - (1 + i)^-n) / i
     */
    public static BigDecimal valorPresente(BigDecimal parcela, BigDecimal taxaMensalPercentual, int parcelas) {
        double pmt = parcela.doubleValue();
        double i = fracao(taxaMensalPercentual);
        if (i == 0) {
            return arredondar(pmt * parcelas);
        }
        return arredondar(pmt * (1 - Math.pow(1 + i, -parcelas)) / i);
    }

    /**
     * Saldo devedor de um financiamento Price depois de pagar "pagas" parcelas:
     *   saldo = PV * ((1+i)^n - (1+i)^k) / ((1+i)^n - 1)
     */
    public static BigDecimal saldoAposParcelas(BigDecimal valor, BigDecimal taxaMensalPercentual,
                                               int parcelasTotal, int pagas) {
        double pv = valor.doubleValue();
        double i = fracao(taxaMensalPercentual);
        if (i == 0) {
            return arredondar(pv * (parcelasTotal - pagas) / parcelasTotal);
        }
        double fatorTotal = Math.pow(1 + i, parcelasTotal);
        double fatorPago = Math.pow(1 + i, pagas);
        return arredondar(pv * (fatorTotal - fatorPago) / (fatorTotal - 1));
    }

    /** Juros de um mês sobre o saldo: saldo * taxa. */
    public static BigDecimal jurosDoMes(BigDecimal saldo, BigDecimal taxaMensalPercentual) {
        return arredondar(saldo.multiply(taxaMensalPercentual).divide(CEM, 10, RoundingMode.HALF_UP));
    }

    /** Quanto "parte" representa de "total", em % com 2 casas. Devolve null se o total for zero ou nulo. */
    public static BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        if (parte == null || total == null || total.signum() == 0) {
            return null;
        }
        return parte.multiply(CEM).divide(total, 2, RoundingMode.HALF_UP);
    }

    /**
     * Converte taxa ANUAL em taxa MENSAL equivalente (juros compostos):
     *   mensal = (1 + anual)^(1/12) - 1
     * Entrada em % (ex.: 13.75) e saída em fração (ex.: 0.0108).
     */
    public static double taxaMensalEquivalente(double taxaAnualPercentual) {
        return Math.pow(1 + taxaAnualPercentual / 100.0, 1.0 / 12.0) - 1;
    }
}
