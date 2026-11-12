package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import br.com.fiap.contacerta.dto.investimento.SimulacaoTesouroResponse;
import br.com.fiap.contacerta.dto.investimento.SimulacaoTesouroResponse.MesSimulacao;

/**
 * Simulador do Tesouro Selic (sem banco de dados: ver TesouroSelicCalculadoraTest).
 *
 * Passo a passo:
 *  1. taxa mensal = (1 + Selic anual)^(1/12) - 1  (juros compostos);
 *  2. cada aporte é um "lote" que rende sozinho desde o mês em que entrou;
 *  3. o Imposto de Renda incide só sobre o RENDIMENTO de cada lote, com alíquota que diminui
 *     conforme o tempo que aquele lote ficou investido (tabela regressiva);
 *  4. descontamos a inflação (IPCA) para mostrar o ganho real e o valor em "dinheiro de hoje".
 *
 * Convenção: os aportes entram no INÍCIO de cada mês; no 1º mês entram o valor inicial e o
 * primeiro aporte. Total investido = valor inicial + aporte x meses.
 */
public final class TesouroSelicCalculadora {

    static final List<String> SIMPLIFICACOES = List.of(
            "IOF ignorado: ele só incide em resgates feitos com menos de 30 dias.",
            "Taxa de custódia da B3 ignorada.",
            "A Selic e o IPCA informados são mantidos iguais durante todo o período.",
            "Cada mês conta como 30 dias na tabela regressiva do Imposto de Renda.",
            "Os aportes entram no início de cada mês; no primeiro mês entram o valor inicial e o primeiro aporte.");

    private TesouroSelicCalculadora() {
    }

    /**
     * Tabela regressiva do IR da renda fixa, pela quantidade de dias investidos:
     *   até 180 dias: 22,5% | 181 a 360: 20% | 361 a 720: 17,5% | acima de 720: 15%
     */
    static double aliquotaIr(int dias) {
        if (dias <= 180) {
            return 0.225;
        }
        if (dias <= 360) {
            return 0.20;
        }
        if (dias <= 720) {
            return 0.175;
        }
        return 0.15;
    }

    public static SimulacaoTesouroResponse simular(BigDecimal valorInicial, BigDecimal aporteMensal, int meses,
                                                   BigDecimal selicAnual, BigDecimal ipcaAnual, String origemDasTaxas) {
        double inicial = valorInicial.doubleValue();
        double aporte = aporteMensal.doubleValue();
        double taxaMensal = CalculadoraFinanceira.taxaMensalEquivalente(selicAnual.doubleValue());
        double ipcaMensal = CalculadoraFinanceira.taxaMensalEquivalente(ipcaAnual.doubleValue());

        // fator[n] = (1 + taxa)^n : quanto R$ 1,00 vira depois de n meses
        double[] fator = new double[meses + 1];
        fator[0] = 1;
        for (int n = 1; n <= meses; n++) {
            fator[n] = fator[n - 1] * (1 + taxaMensal);
        }

        List<MesSimulacao> evolucao = new ArrayList<>();
        double bruto = 0;
        double imposto = 0;
        double investido = 0;

        for (int mes = 1; mes <= meses; mes++) {
            bruto = 0;
            imposto = 0;
            // percorre os lotes: o lote k entrou no início do mês k
            for (int k = 1; k <= mes; k++) {
                double lote = aporte + (k == 1 ? inicial : 0);
                int mesesInvestido = mes - k + 1;
                double valorDoLote = lote * fator[mesesInvestido];
                double rendimentoDoLote = valorDoLote - lote;
                bruto += valorDoLote;
                imposto += rendimentoDoLote * aliquotaIr(mesesInvestido * 30);
            }
            investido = inicial + aporte * mes;
            evolucao.add(new MesSimulacao(mes, dinheiro(investido), dinheiro(bruto),
                    dinheiro(imposto), dinheiro(bruto - imposto)));
        }

        double liquido = bruto - imposto;
        double rendimentoLiquido = liquido - investido;
        double rentabilidadeLiquida = investido == 0 ? 0 : rendimentoLiquido / investido;
        double fatorInflacao = Math.pow(1 + ipcaMensal, meses); // inflação acumulada no período
        double juroReal = (1 + rentabilidadeLiquida) / fatorInflacao - 1;

        return new SimulacaoTesouroResponse(
                valorInicial,
                aporteMensal,
                meses,
                selicAnual,
                ipcaAnual,
                percentual(taxaMensal),
                dinheiro(investido),
                dinheiro(bruto),
                dinheiro(bruto - investido),
                dinheiro(imposto),
                dinheiro(liquido),
                dinheiro(rendimentoLiquido),
                percentual(rentabilidadeLiquida),
                percentual(fatorInflacao - 1),
                percentual(juroReal),
                dinheiro(liquido / fatorInflacao),
                evolucao,
                origemDasTaxas,
                SIMPLIFICACOES);
    }

    private static BigDecimal dinheiro(double valor) {
        return CalculadoraFinanceira.arredondar(valor);
    }

    /** 0.0108 -> 1.08 (%) */
    private static BigDecimal percentual(double fracao) {
        return CalculadoraFinanceira.arredondar(fracao * 100);
    }
}
