package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.stream.Stream;

import br.com.fiap.contacerta.dto.analise.RenegociacaoResponse;
import br.com.fiap.contacerta.dto.analise.RenegociacaoResponse.Cenario;
import br.com.fiap.contacerta.model.Divida;

/**
 * Simulador de renegociação: compara três caminhos para a mesma dívida e diz qual sai mais barato.
 *   1. CONTINUAR         - seguir pagando a parcela atual até acabar;
 *   2. A_VISTA           - pagar tudo de uma vez com o desconto oferecido;
 *   3. NOVO_PARCELAMENTO - reparcelar o saldo com nova taxa e novo prazo (Tabela Price).
 * É só simulação: nada é gravado. Classe sem banco de dados (ver RenegociacaoCalculadoraTest).
 */
public final class RenegociacaoCalculadora {

    /** Limite de 50 anos para o cenário "continuar": se não quitar até lá, consideramos que nunca quita. */
    private static final int LIMITE_MESES = 600;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private RenegociacaoCalculadora() {
    }

    public static RenegociacaoResponse comparar(Divida divida, BigDecimal percentualDesconto,
                                                int novasParcelas, BigDecimal novaTaxa) {
        Cenario continuar = continuarComoEsta(divida);
        Cenario aVista = aVistaComDesconto(divida.getSaldoDevedor(), percentualDesconto);
        Cenario novo = novoParcelamento(divida.getSaldoDevedor(), novasParcelas, novaTaxa);

        // O mais barato entre os cenários em que a dívida realmente acaba.
        Cenario melhor = Stream.of(continuar, aVista, novo)
                .filter(Cenario::quitavel)
                .min(Comparator.comparing(Cenario::totalAPagar))
                .orElse(aVista);

        BigDecimal economia = continuar.quitavel() ? continuar.totalAPagar().subtract(melhor.totalAPagar()) : null;

        String mensagem;
        if (!continuar.quitavel()) {
            mensagem = "Do jeito que está, a parcela não cobre os juros e a dívida nunca acaba. O melhor cenário é \""
                    + melhor.nome() + "\", pagando " + Formatador.reais(melhor.totalAPagar()) + " no total.";
        } else if (melhor == continuar) {
            mensagem = "Nenhuma das propostas é melhor do que continuar pagando como está ("
                    + Formatador.reais(continuar.totalAPagar()) + " no total).";
        } else {
            mensagem = "O cenário mais barato é \"" + melhor.nome() + "\": você paga "
                    + Formatador.reais(melhor.totalAPagar()) + " no total e economiza "
                    + Formatador.reais(economia) + " em relação a continuar como está.";
        }

        return new RenegociacaoResponse(divida.getId(), divida.getCredor(), divida.getSaldoDevedor(),
                continuar, aVista, novo, melhor.codigo(), economia, mensagem);
    }

    /** Segue pagando a parcela atual: a cada mês entram os juros e sai a parcela, até o saldo zerar. */
    static Cenario continuarComoEsta(Divida divida) {
        BigDecimal saldo = divida.getSaldoDevedor();
        BigDecimal totalPago = BigDecimal.ZERO;
        BigDecimal totalJuros = BigDecimal.ZERO;
        int meses = 0;

        while (saldo.signum() > 0 && meses < LIMITE_MESES) {
            meses++;
            BigDecimal juros = CalculadoraFinanceira.jurosDoMes(saldo, divida.getTaxaJurosMensal());
            saldo = saldo.add(juros);
            BigDecimal pago = divida.getValorParcela().min(saldo);
            saldo = saldo.subtract(pago);
            totalPago = totalPago.add(pago);
            totalJuros = totalJuros.add(juros);
        }

        if (saldo.signum() > 0) {
            return new Cenario("CONTINUAR", "Continuar como está", false, null, null, divida.getValorParcela(), null,
                    "A parcela atual não cobre os juros: a dívida não termina.");
        }
        return new Cenario("CONTINUAR", "Continuar como está", true, totalPago, meses, divida.getValorParcela(),
                totalJuros, "Mantendo a parcela e a taxa de " + Formatador.percentual(divida.getTaxaJurosMensal()) + " ao mês.");
    }

    /** Paga tudo hoje com desconto: total = saldo * (1 - desconto). */
    static Cenario aVistaComDesconto(BigDecimal saldo, BigDecimal percentualDesconto) {
        BigDecimal total = saldo.multiply(CEM.subtract(percentualDesconto)).divide(CEM, 2, RoundingMode.HALF_UP);
        return new Cenario("A_VISTA", "Pagar à vista com desconto", true, total, 1, total, BigDecimal.ZERO.setScale(2),
                "Desconto de " + Formatador.reais(saldo.subtract(total)) + " ("
                        + Formatador.percentual(percentualDesconto) + ").");
    }

    /** Reparcela o saldo pela Price: PMT = PV * i / (1 - (1+i)^-n); se i = 0, PMT = PV / n. */
    static Cenario novoParcelamento(BigDecimal saldo, int parcelas, BigDecimal taxa) {
        BigDecimal parcela = CalculadoraFinanceira.parcelaPrice(saldo, taxa, parcelas);
        BigDecimal total = parcela.multiply(BigDecimal.valueOf(parcelas));
        return new Cenario("NOVO_PARCELAMENTO", "Parcelar nas novas condições", true, total, parcelas, parcela,
                total.subtract(saldo).max(BigDecimal.ZERO),
                parcelas + "x de " + Formatador.reais(parcela) + " a " + Formatador.percentual(taxa) + " ao mês.");
    }
}
