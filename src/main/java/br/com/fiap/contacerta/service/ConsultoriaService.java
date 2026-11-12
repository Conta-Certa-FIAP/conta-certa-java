package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dto.analise.ConsultoriaResponse;
import br.com.fiap.contacerta.dto.analise.ConsultoriaResponse.ComparacaoPlanos;
import br.com.fiap.contacerta.dto.analise.ConsultoriaResponse.SugestaoCorte;
import br.com.fiap.contacerta.dto.analise.PlanoQuitacao;
import br.com.fiap.contacerta.service.PlanoQuitacaoCalculadora.Metodo;

/**
 * Consultoria de dívida: responde "quanto devo pagar por mês?" e "em que ordem?".
 *
 *   folga          = renda média - gastos essenciais médios
 *   valor sugerido = o MENOR entre a folga e 40% da renda (limite definido pelo grupo, para
 *                    a pessoa não comprometer quase tudo com dívida e ficar sem nada)
 */
@Service
public class ConsultoriaService {

    /** No máximo 40% da renda vai para dívidas. */
    static final BigDecimal LIMITE_DA_RENDA = new BigDecimal("0.40");

    private static final String COMO_CALCULAMOS = "Renda média = média mensal das entradas dos últimos 3 meses "
            + "(sem resgates de cofre e transferências recebidas) ou, se não houver, a renda declarada. "
            + "Gastos essenciais = média mensal das saídas em categorias essenciais, sem contar pagamento de dívidas. "
            + "Folga = renda média - gastos essenciais. "
            + "Valor sugerido para dívidas = o menor entre a folga e 40% da renda. "
            + "Os planos simulam mês a mês: juros em todas as dívidas, parcela mínima de todas e o que sobra vai "
            + "para a dívida prioritária (avalanche: maior juros; bola de neve: menor saldo), por até 360 meses.";

    private final PerfilFinanceiroService perfilService;

    public ConsultoriaService(PerfilFinanceiroService perfilService) {
        this.perfilService = perfilService;
    }

    public ConsultoriaResponse consultar(Long usuarioId) {
        PerfilFinanceiro perfil = perfilService.montar(usuarioId);
        List<String> avisos = new ArrayList<>();
        List<SugestaoCorte> cortes = sugestoesDeCorte(perfil);
        BigDecimal totalParcelas = perfil.totalParcelas();

        if (!perfil.temRenda()) {
            // Sem renda não dá para sugerir valor: devolvemos o que temos e pedimos a renda.
            avisos.add(SituacaoService.AVISO_SEM_RENDA);
            return new ConsultoriaResponse(null, perfil.gastosEssenciaisMedia(), null, null, null, totalParcelas,
                    null, null, null, cortes, COMO_CALCULAMOS, avisos);
        }

        BigDecimal renda = perfil.rendaMedia();
        BigDecimal folga = renda.subtract(perfil.gastosEssenciaisMedia());
        BigDecimal limite = limiteDaRenda(renda);
        BigDecimal sugerido = valorSugerido(perfil);

        if (folga.signum() <= 0) {
            avisos.add("Suas despesas essenciais (" + Formatador.reais(perfil.gastosEssenciaisMedia())
                    + " por mês) já superam a sua renda (" + Formatador.reais(renda)
                    + "). Antes de assumir novos pagamentos, procure renegociar as dívidas e rever os gastos fixos.");
        }

        PlanoQuitacao avalanche = null;
        PlanoQuitacao bolaDeNeve = null;
        ComparacaoPlanos comparacao = null;

        if (perfil.dividasAtivas().isEmpty()) {
            avisos.add("Você não tem dívidas ativas. Que tal guardar o valor sugerido em um cofre?");
        } else if (sugerido.signum() > 0) {
            avalanche = PlanoQuitacaoCalculadora.simular(perfil.dividasAtivas(), sugerido, Metodo.AVALANCHE);
            bolaDeNeve = PlanoQuitacaoCalculadora.simular(perfil.dividasAtivas(), sugerido, Metodo.BOLA_DE_NEVE);
            comparacao = comparar(avalanche, bolaDeNeve);
            if (sugerido.compareTo(totalParcelas) < 0) {
                avisos.add("Suas parcelas somam " + Formatador.reais(totalParcelas) + " por mês, acima do valor sugerido de "
                        + Formatador.reais(sugerido) + ". Vale renegociar para alongar o prazo ou reduzir os juros.");
            }
        }

        return new ConsultoriaResponse(renda, perfil.gastosEssenciaisMedia(), folga, limite, sugerido, totalParcelas,
                avalanche, bolaDeNeve, comparacao, cortes, COMO_CALCULAMOS, avisos);
    }

    // ------------------------------------------------------------------ cálculos (estáticos e testáveis)

    static BigDecimal limiteDaRenda(BigDecimal renda) {
        return renda.multiply(LIMITE_DA_RENDA).setScale(2, RoundingMode.HALF_UP);
    }

    /** O menor entre a folga e 40% da renda. Zero se não houver renda conhecida ou folga. */
    static BigDecimal valorSugerido(PerfilFinanceiro perfil) {
        if (!perfil.temRenda()) {
            return BigDecimal.ZERO;
        }
        BigDecimal folga = perfil.rendaMedia().subtract(perfil.gastosEssenciaisMedia());
        if (folga.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return folga.min(limiteDaRenda(perfil.rendaMedia())).setScale(2, RoundingMode.HALF_UP);
    }

    /** As 3 categorias não essenciais com mais gasto no último mês e a economia cortando 50%. */
    static List<SugestaoCorte> sugestoesDeCorte(PerfilFinanceiro perfil) {
        return perfil.naoEssenciaisUltimoMes().stream()
                .limit(3)
                .map(total -> new SugestaoCorte(total.categoria(), total.total(), metade(total.total())))
                .toList();
    }

    static ComparacaoPlanos comparar(PlanoQuitacao avalanche, PlanoQuitacao bolaDeNeve) {
        BigDecimal economia = bolaDeNeve.totalPago().subtract(avalanche.totalPago());
        int meses = bolaDeNeve.meses() - avalanche.meses();

        String mensagem;
        if (!avalanche.quitaTudo() || !bolaDeNeve.quitaTudo()) {
            mensagem = "Com o valor sugerido as dívidas não são quitadas em 30 anos. Renegociar é o melhor caminho.";
        } else if (economia.signum() > 0) {
            mensagem = "Pelo método avalanche você paga " + Formatador.reais(economia)
                    + " a menos do que pelo bola de neve.";
        } else if (economia.signum() < 0) {
            mensagem = "Neste caso o bola de neve sai " + Formatador.reais(economia.negate())
                    + " mais barato do que o avalanche.";
        } else {
            mensagem = "Os dois métodos dão o mesmo resultado para as suas dívidas.";
        }
        return new ComparacaoPlanos(economia, meses, mensagem);
    }

    /**
     * Frase de impacto do resumo mensal: refaz o plano avalanche somando ao pagamento mensal
     * metade do que foi gasto na categoria e vê quantos meses a quitação adianta.
     */
    static String fraseDeImpacto(PerfilFinanceiro perfil, String categoria, BigDecimal gastoNaCategoria) {
        BigDecimal economia = metade(gastoNaCategoria);
        String inicio = "Cortando metade dos gastos com " + categoria + ", você ";

        if (perfil.dividasAtivas().isEmpty()) {
            return inicio + "guarda " + Formatador.reais(economia) + " por mês.";
        }

        // Base: o que a pessoa já paga hoje (as parcelas) ou o valor sugerido, o que for maior.
        BigDecimal base = valorSugerido(perfil).max(perfil.totalParcelas());
        PlanoQuitacao hoje = PlanoQuitacaoCalculadora.simular(perfil.dividasAtivas(), base, Metodo.AVALANCHE);
        PlanoQuitacao comCorte = PlanoQuitacaoCalculadora.simular(perfil.dividasAtivas(), base.add(economia), Metodo.AVALANCHE);

        if (!comCorte.quitaTudo()) {
            return inicio + "libera " + Formatador.reais(economia) + " por mês para negociar suas dívidas.";
        }
        if (!hoje.quitaTudo()) {
            return inicio + "consegue quitar suas dívidas em " + meses(comCorte.meses()) + ".";
        }
        int adiantamento = hoje.meses() - comCorte.meses();
        if (adiantamento > 0) {
            return inicio + "quita suas dívidas " + meses(adiantamento) + " antes.";
        }
        BigDecimal jurosEconomizados = hoje.totalJuros().subtract(comCorte.totalJuros());
        if (jurosEconomizados.signum() > 0) {
            return inicio + "economiza " + Formatador.reais(jurosEconomizados) + " em juros das suas dívidas.";
        }
        return inicio + "libera " + Formatador.reais(economia) + " por mês.";
    }

    private static String meses(int quantidade) {
        return quantidade == 1 ? "1 mês" : quantidade + " meses";
    }

    private static BigDecimal metade(BigDecimal valor) {
        return valor.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }
}
