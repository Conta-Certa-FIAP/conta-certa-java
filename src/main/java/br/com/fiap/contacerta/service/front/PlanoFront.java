package br.com.fiap.contacerta.service.front;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import br.com.fiap.contacerta.dto.analise.PlanoQuitacao;
import br.com.fiap.contacerta.dto.analise.PlanoQuitacao.QuitacaoDivida;
import br.com.fiap.contacerta.dto.front.PlanDto;
import br.com.fiap.contacerta.dto.front.PlanDto.Option;
import br.com.fiap.contacerta.dto.front.PlanDto.Step;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.service.Formatador;
import br.com.fiap.contacerta.service.PlanoQuitacaoCalculadora;
import br.com.fiap.contacerta.service.PlanoQuitacaoCalculadora.Metodo;

/**
 * Monta a tela "Meu plano" a partir da simulação que já existia (PlanoQuitacaoCalculadora).
 * Só métodos estáticos e sem banco: recebe as dívidas, o valor mensal e o "mês atual".
 */
public final class PlanoFront {

    private PlanoFront() {
    }

    public static PlanDto montar(List<Divida> dividasAtivas, BigDecimal valorMensal, Metodo metodo, YearMonth mesAtual) {
        if (dividasAtivas.isEmpty()) {
            return new PlanDto(valorMensal, metodo.name(), null, 100, BigDecimal.ZERO, BigDecimal.ZERO,
                    List.of(), List.of(), null);
        }

        PlanoQuitacao avalanche = PlanoQuitacaoCalculadora.simular(dividasAtivas, valorMensal, Metodo.AVALANCHE);
        PlanoQuitacao bolaDeNeve = PlanoQuitacaoCalculadora.simular(dividasAtivas, valorMensal, Metodo.BOLA_DE_NEVE);
        PlanoQuitacao escolhido = metodo == Metodo.AVALANCHE ? avalanche : bolaDeNeve;
        Metodo recomendado = recomendado(avalanche, bolaDeNeve);

        BigDecimal pago = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (Divida divida : dividasAtivas) {
            pago = pago.add(DividaFront.pago(divida));
            total = total.add(divida.getValorOriginal());
        }

        List<Option> opcoes = List.of(
                opcao(avalanche, mesAtual, recomendado == Metodo.AVALANCHE),
                opcao(bolaDeNeve, mesAtual, recomendado == Metodo.BOLA_DE_NEVE));

        return new PlanDto(valorMensal, metodo.name(), mesLivre(escolhido, mesAtual), DividaFront.percentual(pago, total),
                pago, total, ordem(escolhido, dividasAtivas, mesAtual), opcoes, aviso(escolhido, dividasAtivas, valorMensal));
    }

    /**
     * O método que paga menos juros entre os que quitam tudo. Empate (ou nenhum quita): avalanche,
     * que é o que mais economiza juros em geral.
     */
    static Metodo recomendado(PlanoQuitacao avalanche, PlanoQuitacao bolaDeNeve) {
        if (avalanche.quitaTudo() && !bolaDeNeve.quitaTudo()) {
            return Metodo.AVALANCHE;
        }
        if (bolaDeNeve.quitaTudo() && !avalanche.quitaTudo()) {
            return Metodo.BOLA_DE_NEVE;
        }
        return bolaDeNeve.totalJuros().compareTo(avalanche.totalJuros()) < 0 ? Metodo.BOLA_DE_NEVE : Metodo.AVALANCHE;
    }

    /** Mês em que a última dívida acaba ("AAAA-MM"); nulo se o plano não quita tudo. */
    static String mesLivre(PlanoQuitacao plano, YearMonth mesAtual) {
        return plano.quitaTudo() ? mesAtual.plusMonths(plano.meses()).toString() : null;
    }

    /** As dívidas quitadas na ordem do plano e, depois, as que não acabam (sem data). */
    static List<Step> ordem(PlanoQuitacao plano, List<Divida> dividas, YearMonth mesAtual) {
        Map<Long, Divida> porId = new HashMap<>();
        dividas.forEach(divida -> porId.put(divida.getId(), divida));

        List<Step> passos = new ArrayList<>();
        for (QuitacaoDivida quitacao : plano.ordemQuitacao()) {
            Divida divida = porId.get(quitacao.dividaId());
            if (divida != null) {
                passos.add(passo(passos.size() + 1, divida, mesAtual.plusMonths(quitacao.mes()).toString()));
            }
        }
        Set<Long> jaListadas = plano.ordemQuitacao().stream().map(QuitacaoDivida::dividaId).collect(Collectors.toSet());
        for (Divida divida : dividas) {
            if (!jaListadas.contains(divida.getId())) {
                passos.add(passo(passos.size() + 1, divida, null));
            }
        }
        return passos;
    }

    private static Step passo(int posicao, Divida divida, String mesQuitacao) {
        return new Step(posicao, divida.getId(), DividaFront.nome(divida), mesQuitacao,
                divida.getSaldoDevedor(), DividaFront.progresso(divida));
    }

    private static Option opcao(PlanoQuitacao plano, YearMonth mesAtual, boolean recomendado) {
        return new Option(plano.metodo(), mesLivre(plano, mesAtual), plano.totalJuros(), recomendado);
    }

    /** O aviso da simulação e, se for o caso, o aviso de que o valor não cobre nem as parcelas. */
    static String aviso(PlanoQuitacao plano, List<Divida> dividas, BigDecimal valorMensal) {
        BigDecimal parcelas = dividas.stream().map(Divida::getValorParcela).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> avisos = new ArrayList<>();
        if (valorMensal.compareTo(parcelas) < 0) {
            avisos.add("As parcelas das suas dívidas somam " + Formatador.reais(parcelas)
                    + " por mês, mais do que " + Formatador.reais(valorMensal)
                    + ". Alguma parcela vai atrasar: vale renegociar o prazo.");
        }
        if (plano.aviso() != null) {
            avisos.add(plano.aviso());
        }
        return avisos.isEmpty() ? null : String.join(" ", avisos);
    }
}
