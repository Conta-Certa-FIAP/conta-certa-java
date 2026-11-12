package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import br.com.fiap.contacerta.dto.analise.ProjecaoDividaResponse;
import br.com.fiap.contacerta.dto.analise.ProjecaoDividaResponse.EncargosAtraso;
import br.com.fiap.contacerta.dto.analise.ProjecaoDividaResponse.MesProjecao;
import br.com.fiap.contacerta.model.Divida;

/**
 * "Correção de endividamento": mostra como UMA dívida evolui nos próximos meses em dois cenários:
 *  - pagando a parcela todo mês;
 *  - sem pagar nada (o saldo cresce com juros sobre juros).
 * Se a dívida estiver atrasada, calcula também a multa e os juros de mora da parcela vencida.
 * Classe sem banco de dados (ver ProjecaoDividaCalculadoraTest).
 */
public final class ProjecaoDividaCalculadora {

    /** Multa por atraso: 2% sobre a parcela vencida. */
    static final BigDecimal MULTA = new BigDecimal("0.02");
    /** Juros de mora: 1% ao mês, proporcional aos dias de atraso. */
    static final BigDecimal MORA_AO_MES = new BigDecimal("0.01");

    private static final String BASE_LEGAL = "Multa de mora limitada a 2% pelo art. 52, §1º, do Código de Defesa do "
            + "Consumidor; juros de mora de 1% ao mês, proporcionais aos dias de atraso, percentual usual nos "
            + "contratos de consumo. Valores estimados: o contrato pode prever outros encargos.";

    private ProjecaoDividaCalculadora() {
    }

    public static ProjecaoDividaResponse projetar(Divida divida, int meses) {
        List<String> avisos = new ArrayList<>();
        BigDecimal saldo = divida.getSaldoDevedor();
        BigDecimal taxa = divida.getTaxaJurosMensal();

        List<MesProjecao> pagando = cenario(saldo, taxa, divida.getValorParcela(), meses);
        List<MesProjecao> semPagar = cenario(saldo, taxa, BigDecimal.ZERO, meses);

        BigDecimal saldoFinalPagando = saldoFinal(pagando, saldo);
        BigDecimal saldoFinalSemPagar = saldoFinal(semPagar, saldo);
        BigDecimal totalJurosPagando = pagando.stream().map(MesProjecao::juros).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (saldo.signum() == 0) {
            avisos.add("Esta dívida já está quitada: não há o que projetar.");
        } else {
            BigDecimal jurosDoMes = CalculadoraFinanceira.jurosDoMes(saldo, taxa);
            if (divida.getValorParcela().compareTo(jurosDoMes) <= 0) {
                avisos.add("A parcela de " + Formatador.reais(divida.getValorParcela()) + " não cobre os juros do mês ("
                        + Formatador.reais(jurosDoMes) + "): mesmo pagando, a dívida não diminui. Renegocie.");
            } else if (saldoFinalPagando.signum() == 0) {
                avisos.add("Pagando a parcela todo mês, a dívida acaba em " + pagando.size() + " mês(es).");
            }
        }

        return new ProjecaoDividaResponse(
                divida.getId(),
                divida.getCredor(),
                divida.getTipo().getDescricao(),
                saldo,
                taxa,
                divida.getValorParcela(),
                meses,
                pagando,
                semPagar,
                totalJurosPagando,
                saldoFinalPagando,
                saldoFinalSemPagar,
                saldoFinalSemPagar.subtract(saldo),
                encargosDoAtraso(divida),
                avisos);
    }

    /**
     * Um cenário mês a mês. Em cada mês:
     *   juros = saldo inicial * taxa;  saldo final = saldo inicial + juros - parcela.
     * A última parcela é menor (só o que falta). O cenário para quando o saldo zera.
     */
    static List<MesProjecao> cenario(BigDecimal saldoInicial, BigDecimal taxa, BigDecimal parcela, int meses) {
        List<MesProjecao> linhas = new ArrayList<>();
        BigDecimal saldo = saldoInicial;
        for (int mes = 1; mes <= meses && saldo.signum() > 0; mes++) {
            BigDecimal juros = CalculadoraFinanceira.jurosDoMes(saldo, taxa);
            BigDecimal comJuros = saldo.add(juros);
            BigDecimal pago = parcela.min(comJuros);
            BigDecimal saldoFinal = comJuros.subtract(pago);
            linhas.add(new MesProjecao(mes, saldo, juros, taxa, pago, saldoFinal));
            saldo = saldoFinal;
        }
        return linhas;
    }

    private static BigDecimal saldoFinal(List<MesProjecao> linhas, BigDecimal saldoSeVazio) {
        return linhas.isEmpty() ? saldoSeVazio : linhas.get(linhas.size() - 1).saldoFinal();
    }

    /**
     * Encargos da parcela vencida (só para dívida atrasada; senão devolve null):
     *   multa         = 2% da parcela
     *   juros de mora = parcela * 1% * (dias de atraso / 30)
     *   para quitar hoje = saldo devedor + multa + juros de mora
     */
    static EncargosAtraso encargosDoAtraso(Divida divida) {
        if (!AnaliseFinanceira.atrasada(divida) || divida.getDiasAtraso() <= 0) {
            return null;
        }
        BigDecimal parcela = divida.getValorParcela();
        BigDecimal multa = parcela.multiply(MULTA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal mora = parcela.multiply(MORA_AO_MES)
                .multiply(BigDecimal.valueOf(divida.getDiasAtraso()))
                .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);

        return new EncargosAtraso(
                divida.getDiasAtraso(),
                multa,
                mora,
                parcela.add(multa).add(mora),
                divida.getSaldoDevedor().add(multa).add(mora),
                BASE_LEGAL);
    }
}
