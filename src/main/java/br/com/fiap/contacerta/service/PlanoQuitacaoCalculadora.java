package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import br.com.fiap.contacerta.dto.analise.PlanoQuitacao;
import br.com.fiap.contacerta.dto.analise.PlanoQuitacao.MesPlano;
import br.com.fiap.contacerta.dto.analise.PlanoQuitacao.QuitacaoDivida;
import br.com.fiap.contacerta.model.Divida;

/**
 * Simula, mês a mês, a quitação de várias dívidas com um valor fixo por mês.
 *
 * Dois métodos conhecidos de educação financeira:
 *  - AVALANCHE: ataca primeiro a dívida com os JUROS MAIS ALTOS (paga-se menos juros no total);
 *  - BOLA_DE_NEVE: ataca primeiro a dívida de MENOR SALDO (dá a sensação de vitória mais cedo).
 *
 * A cada mês:
 *  1. aplica os juros em todas as dívidas;
 *  2. paga a parcela mínima de todas (na ordem de prioridade, enquanto houver dinheiro);
 *  3. o que sobrar vai para a dívida prioritária; quando uma zera, a sobra passa para a próxima.
 *
 * Classe sem banco de dados: recebe as dívidas e devolve o plano (ver PlanoQuitacaoCalculadoraTest).
 */
public final class PlanoQuitacaoCalculadora {

    public enum Metodo {
        AVALANCHE("Maior juros primeiro: paga menos juros no total."),
        BOLA_DE_NEVE("Menor saldo primeiro: elimina dívidas mais rápido e dá motivação.");

        private final String descricao;

        Metodo(String descricao) {
            this.descricao = descricao;
        }
    }

    /** Limite de 30 anos: se não quitar até lá, o valor mensal não é suficiente. */
    public static final int LIMITE_MESES = 360;

    private PlanoQuitacaoCalculadora() {
    }

    /** Cópia "de trabalho" de uma dívida: o saldo vai mudando durante a simulação. */
    private static final class Item {
        final Long id;
        final String credor;
        final BigDecimal taxa;
        final BigDecimal parcela;
        BigDecimal saldo;
        boolean quitada;

        Item(Divida divida) {
            this.id = divida.getId();
            this.credor = divida.getCredor();
            this.taxa = divida.getTaxaJurosMensal();
            this.parcela = divida.getValorParcela();
            this.saldo = divida.getSaldoDevedor();
        }
    }

    public static PlanoQuitacao simular(List<Divida> dividas, BigDecimal valorMensal, Metodo metodo) {
        List<Item> itens = dividas.stream()
                .filter(d -> d.getSaldoDevedor().signum() > 0)
                .map(Item::new)
                .sorted(ordemDePrioridade(metodo))
                .toList();

        List<QuitacaoDivida> ordemQuitacao = new ArrayList<>();
        List<MesPlano> evolucao = new ArrayList<>();
        BigDecimal totalPago = BigDecimal.ZERO;
        BigDecimal totalJuros = BigDecimal.ZERO;
        BigDecimal jurosDoPrimeiroMes = BigDecimal.ZERO;
        int mes = 0;

        while (existeSaldo(itens) && mes < LIMITE_MESES) {
            mes++;

            // 1) juros do mês em todas as dívidas ainda abertas
            BigDecimal jurosDoMes = BigDecimal.ZERO;
            for (Item item : itens) {
                if (!item.quitada) {
                    BigDecimal juros = CalculadoraFinanceira.jurosDoMes(item.saldo, item.taxa);
                    item.saldo = item.saldo.add(juros);
                    jurosDoMes = jurosDoMes.add(juros);
                }
            }
            if (mes == 1) {
                jurosDoPrimeiroMes = jurosDoMes;
            }

            // 2) parcela mínima de todas, na ordem de prioridade
            BigDecimal disponivel = valorMensal;
            for (Item item : itens) {
                BigDecimal pagamento = item.parcela.min(item.saldo).min(disponivel);
                disponivel = pagar(item, pagamento, disponivel);
            }
            // 3) a sobra vai para a prioritária (e, se ela zerar, para a próxima)
            for (Item item : itens) {
                BigDecimal pagamento = item.saldo.min(disponivel);
                disponivel = pagar(item, pagamento, disponivel);
            }

            BigDecimal pagoNoMes = valorMensal.subtract(disponivel);
            totalPago = totalPago.add(pagoNoMes);
            totalJuros = totalJuros.add(jurosDoMes);

            // registra as dívidas que acabaram neste mês
            for (Item item : itens) {
                if (!item.quitada && item.saldo.signum() == 0) {
                    item.quitada = true;
                    ordemQuitacao.add(new QuitacaoDivida(item.id, item.credor, mes));
                }
            }
            evolucao.add(new MesPlano(mes, jurosDoMes, pagoNoMes, saldoTotal(itens)));
        }

        boolean quitaTudo = !existeSaldo(itens);
        String aviso = null;
        if (!quitaTudo) {
            aviso = "Com " + Formatador.reais(valorMensal) + " por mês as dívidas não são quitadas em 30 anos. "
                    + "Só de juros elas geram " + Formatador.reais(jurosDoPrimeiroMes)
                    + " no primeiro mês. Procure renegociar para reduzir os juros.";
        }
        return new PlanoQuitacao(metodo.name(), metodo.descricao, quitaTudo, mes,
                totalPago, totalJuros, ordemQuitacao, evolucao, aviso);
    }

    /** Paga "pagamento" na dívida e devolve quanto ainda sobra disponível no mês. */
    private static BigDecimal pagar(Item item, BigDecimal pagamento, BigDecimal disponivel) {
        if (item.quitada || pagamento.signum() <= 0) {
            return disponivel;
        }
        item.saldo = item.saldo.subtract(pagamento);
        return disponivel.subtract(pagamento);
    }

    private static Comparator<Item> ordemDePrioridade(Metodo metodo) {
        Comparator<Item> maiorTaxa = Comparator.comparing((Item i) -> i.taxa).reversed();
        Comparator<Item> menorSaldo = Comparator.comparing((Item i) -> i.saldo);
        return metodo == Metodo.AVALANCHE
                ? maiorTaxa.thenComparing(menorSaldo)
                : menorSaldo.thenComparing(maiorTaxa);
    }

    private static boolean existeSaldo(List<Item> itens) {
        return itens.stream().anyMatch(item -> item.saldo.signum() > 0);
    }

    private static BigDecimal saldoTotal(List<Item> itens) {
        return itens.stream().map(item -> item.saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
