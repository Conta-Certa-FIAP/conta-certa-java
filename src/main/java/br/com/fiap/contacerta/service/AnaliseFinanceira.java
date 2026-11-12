package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import br.com.fiap.contacerta.dao.TotalCategoria;
import br.com.fiap.contacerta.dto.analise.ItemNota;
import br.com.fiap.contacerta.model.CategoriaPadrao;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.SituacaoFinanceira;
import br.com.fiap.contacerta.model.StatusDivida;
import br.com.fiap.contacerta.model.TipoMovimento;

/**
 * Os CÁLCULOS da análise financeira: renda média, gastos essenciais, comprometimento,
 * situação e nota de saúde.
 *
 * Tudo aqui é método estático que recebe números prontos e devolve o resultado, sem tocar
 * no banco de dados. Assim cada regra pode ser testada sozinha (ver AnaliseFinanceiraTest).
 * Quem busca os dados no banco é o PerfilFinanceiroService.
 */
public final class AnaliseFinanceira {

    /** As médias olham os últimos 3 meses. */
    public static final int MESES_DA_MEDIA = 3;

    /** Entradas que não são renda de verdade: dinheiro que só mudou de lugar. */
    private static final Set<String> ENTRADAS_QUE_NAO_SAO_RENDA =
            Set.of(CategoriaPadrao.RESGATE_COFRE, CategoriaPadrao.TRANSFERENCIA_RECEBIDA);

    private AnaliseFinanceira() {
    }

    // ------------------------------------------------------------------ médias

    /**
     * Por quantos meses dividir as somas para tirar a média mensal.
     * Normalmente 3. Mas quem começou a usar o app há pouco tem menos histórico: se a primeira
     * transação é de 10 dias atrás, dividir por 3 daria uma renda 3 vezes menor que a real.
     * Regra: 1 mês para cada 30 dias de histórico (arredondando para cima), entre 1 e 3.
     */
    public static int mesesDeHistorico(LocalDate primeiraTransacao, LocalDate hoje) {
        if (primeiraTransacao == null) {
            return MESES_DA_MEDIA;
        }
        long dias = ChronoUnit.DAYS.between(primeiraTransacao, hoje) + 1;
        int meses = (int) Math.ceil(dias / 30.0);
        return Math.max(1, Math.min(MESES_DA_MEDIA, meses));
    }

    /** Média mensal das ENTRADAS, sem contar "Resgate do cofre" e "Transferência recebida". */
    public static BigDecimal rendaMedia(List<TotalCategoria> totais, int meses) {
        BigDecimal soma = totais.stream()
                .filter(t -> t.tipo() == TipoMovimento.ENTRADA)
                .filter(t -> !ENTRADAS_QUE_NAO_SAO_RENDA.contains(t.categoria()))
                .map(TotalCategoria::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return media(soma, meses);
    }

    /** Média mensal das SAÍDAS em categorias essenciais, sem contar "Pagamento de dívidas". */
    public static BigDecimal gastosEssenciaisMedia(List<TotalCategoria> totais, int meses) {
        BigDecimal soma = totais.stream()
                .filter(t -> t.tipo() == TipoMovimento.SAIDA && t.essencial())
                .filter(t -> !t.categoria().equals(CategoriaPadrao.PAGAMENTO_DIVIDAS))
                .map(TotalCategoria::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return media(soma, meses);
    }

    /**
     * Saídas NÃO essenciais (o que dá para cortar), da maior para a menor.
     * "Depósito no cofre" fica de fora: guardar dinheiro não é gasto.
     */
    public static List<TotalCategoria> naoEssenciais(List<TotalCategoria> totais) {
        return totais.stream()
                .filter(t -> t.tipo() == TipoMovimento.SAIDA && !t.essencial())
                .filter(t -> !t.categoria().equals(CategoriaPadrao.DEPOSITO_COFRE))
                .sorted((a, b) -> b.total().compareTo(a.total()))
                .toList();
    }

    private static BigDecimal media(BigDecimal soma, int meses) {
        return soma.divide(BigDecimal.valueOf(meses), 2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ comprometimento e situação

    /** Comprometimento da renda em %: soma das parcelas / renda média. Nulo se a renda for desconhecida. */
    public static BigDecimal comprometimento(BigDecimal totalParcelas, BigDecimal rendaMedia) {
        return CalculadoraFinanceira.percentual(totalParcelas, rendaMedia);
    }

    public static boolean atrasada(Divida divida) {
        return divida.getStatus() == StatusDivida.ATRASADA || divida.getDiasAtraso() > 0;
    }

    /**
     * Classifica a situação. Testamos da mais grave para a mais leve e devolvemos a primeira
     * que se aplica; assim "vale a situação mais grave".
     *   INADIMPLENTE: alguma dívida atrasada há mais de 90 dias
     *   EM_RISCO:     comprometimento > 50% OU alguma dívida atrasada (até 90 dias)
     *   ATENCAO:      comprometimento > 30% OU dívida ativa de rotativo/cheque especial
     *   CONFORTAVEL:  nenhuma das anteriores
     */
    public static SituacaoFinanceira classificar(BigDecimal comprometimentoPercentual, List<Divida> dividasAtivas) {
        double comprometimento = comprometimentoPercentual == null ? 0 : comprometimentoPercentual.doubleValue();
        boolean atrasoGrave = dividasAtivas.stream().anyMatch(d -> d.getDiasAtraso() > 90);
        boolean algumAtraso = dividasAtivas.stream().anyMatch(AnaliseFinanceira::atrasada);
        boolean temRotativo = dividasAtivas.stream().anyMatch(d -> d.getTipo().isRotativo());

        if (atrasoGrave) {
            return SituacaoFinanceira.INADIMPLENTE;
        }
        if (comprometimento > 50 || algumAtraso) {
            return SituacaoFinanceira.EM_RISCO;
        }
        if (comprometimento > 30 || temRotativo) {
            return SituacaoFinanceira.ATENCAO;
        }
        return SituacaoFinanceira.CONFORTAVEL;
    }

    // ------------------------------------------------------------------ nota de saúde financeira

    /** A nota (0 a 100) e a lista do que somou ou tirou pontos. */
    public record NotaSaude(int nota, List<ItemNota> itens) {
    }

    public static final String COMO_CALCULAMOS_A_NOTA = "A nota começa em 100. "
            + "Comprometimento da renda com parcelas: sem desconto até 30%; daí em diante o desconto cresce "
            + "em linha reta até -40 pontos quando chega a 80% ou mais. "
            + "Atraso: -25 se houver dívida atrasada até 90 dias, ou -40 se houver atraso acima de 90 dias "
            + "(vale só o maior). "
            + "-10 se houver cartão rotativo ou cheque especial ativo. "
            + "-10 se os gastos não essenciais do último mês passarem de 30% da renda. "
            + "-5 se o saldo das contas for menor que 10% da renda. "
            + "+5 se o total guardado nos cofres for igual ou maior que 1 renda mensal. "
            + "O resultado fica sempre entre 0 e 100.";

    /**
     * Calcula a nota. Itens que dependem da renda são pulados quando ela é desconhecida
     * (comprometimentoPercentual e rendaMedia nulos).
     */
    public static NotaSaude calcularNota(BigDecimal comprometimentoPercentual, List<Divida> dividasAtivas,
                                         BigDecimal rendaMedia, BigDecimal naoEssenciaisUltimoMes,
                                         BigDecimal saldoContas, BigDecimal totalCofres) {
        List<ItemNota> itens = new ArrayList<>();
        int nota = 100;

        // 1) Comprometimento: 0 até 30%, linear até -40 em 80%
        if (comprometimentoPercentual != null) {
            int desconto = descontoPorComprometimento(comprometimentoPercentual.doubleValue());
            if (desconto > 0) {
                itens.add(new ItemNota("Parcelas comprometem "
                        + Formatador.percentual(comprometimentoPercentual) + " da renda", -desconto));
                nota -= desconto;
            }
        }

        // 2) Atraso: só o maior dos dois descontos
        boolean atrasoGrave = dividasAtivas.stream().anyMatch(d -> d.getDiasAtraso() > 90);
        boolean algumAtraso = dividasAtivas.stream().anyMatch(AnaliseFinanceira::atrasada);
        if (atrasoGrave) {
            itens.add(new ItemNota("Dívida atrasada há mais de 90 dias", -40));
            nota -= 40;
        } else if (algumAtraso) {
            itens.add(new ItemNota("Dívida atrasada (até 90 dias)", -25));
            nota -= 25;
        }

        // 3) Crédito rotativo
        if (dividasAtivas.stream().anyMatch(d -> d.getTipo().isRotativo())) {
            itens.add(new ItemNota("Cartão rotativo ou cheque especial em uso", -10));
            nota -= 10;
        }

        if (rendaMedia != null && rendaMedia.signum() > 0) {
            // 4) Gastos não essenciais acima de 30% da renda
            if (naoEssenciaisUltimoMes.compareTo(fracaoDe(rendaMedia, "0.30")) > 0) {
                itens.add(new ItemNota("Gastos não essenciais do último mês acima de 30% da renda", -10));
                nota -= 10;
            }
            // 5) Reserva em conta menor que 10% da renda
            if (saldoContas.compareTo(fracaoDe(rendaMedia, "0.10")) < 0) {
                itens.add(new ItemNota("Saldo das contas menor que 10% da renda", -5));
                nota -= 5;
            }
            // 6) Bônus por ter pelo menos 1 renda guardada
            if (totalCofres.compareTo(rendaMedia) >= 0) {
                itens.add(new ItemNota("Cofres com pelo menos 1 renda mensal guardada", 5));
                nota += 5;
            }
        }

        return new NotaSaude(Math.max(0, Math.min(100, nota)), itens);
    }

    /** Desconto linear: 30% -> 0 pontos ... 80% -> 40 pontos (regra de três entre esses dois pontos). */
    static int descontoPorComprometimento(double comprometimento) {
        if (comprometimento <= 30) {
            return 0;
        }
        if (comprometimento >= 80) {
            return 40;
        }
        return (int) Math.round(40 * (comprometimento - 30) / 50);
    }

    private static BigDecimal fracaoDe(BigDecimal valor, String fracao) {
        return valor.multiply(new BigDecimal(fracao));
    }
}
