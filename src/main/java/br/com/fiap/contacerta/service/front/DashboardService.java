package br.com.fiap.contacerta.service.front;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.FiltroTransacao;
import br.com.fiap.contacerta.dao.TotalCategoria;
import br.com.fiap.contacerta.dao.TransacaoDAO;
import br.com.fiap.contacerta.dto.front.DashboardDto;
import br.com.fiap.contacerta.dto.front.DashboardDto.BalancePoint;
import br.com.fiap.contacerta.dto.front.DashboardDto.CategoryExpense;
import br.com.fiap.contacerta.dto.front.DashboardDto.InterestAlert;
import br.com.fiap.contacerta.dto.front.DashboardDto.UpcomingPayment;
import br.com.fiap.contacerta.dto.front.PlanDto;
import br.com.fiap.contacerta.model.CategoriaPadrao;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.TipoMovimento;
import br.com.fiap.contacerta.model.Transacao;
import br.com.fiap.contacerta.service.PerfilFinanceiro;
import br.com.fiap.contacerta.service.PerfilFinanceiroService;
import br.com.fiap.contacerta.service.Periodo;

/** GET /dashboard?month=AAAA-MM: junta tudo o que a tela Início mostra numa resposta só. */
@Service
public class DashboardService {

    /** Quantos vencimentos aparecem em "Próximos pagamentos". */
    static final int MAXIMO_DE_PAGAMENTOS = 5;

    private final PerfilFinanceiroService perfilService;
    private final TransacaoDAO transacaoDAO;
    private final PlanoFrontService planoService;
    private final TrilhaFrontService trilhaService;

    public DashboardService(PerfilFinanceiroService perfilService, TransacaoDAO transacaoDAO,
                            PlanoFrontService planoService, TrilhaFrontService trilhaService) {
        this.perfilService = perfilService;
        this.transacaoDAO = transacaoDAO;
        this.planoService = planoService;
        this.trilhaService = trilhaService;
    }

    public DashboardDto montar(Long usuarioId, String month) {
        YearMonth mes = MesFront.ler(month);
        LocalDate hoje = LocalDate.now();
        PerfilFinanceiro perfil = perfilService.montar(usuarioId); // 404 se o usuário não existir
        BigDecimal saldoHoje = perfil.saldoContas();
        List<Divida> dividas = perfil.dividasAtivas();

        // Entradas, saídas e gastos por categoria do mês escolhido
        Periodo periodo = Periodo.doMes(mes);
        List<TotalCategoria> totais = transacaoDAO.totaisPorCategoria(usuarioId, periodo.inicio(), periodo.fimExclusivo());

        // Gráfico: o mês escolhido até hoje. Mês futuro não tem pontos; previsão só no mês atual.
        List<BalancePoint> historico = List.of();
        List<BalancePoint> previsao = List.of();
        if (!mes.atDay(1).isAfter(hoje)) {
            List<Transacao> desdeOInicioDoMes = transacaoDAO.listar(
                    new FiltroTransacao(usuarioId, null, mes.atDay(1), hoje.plusDays(1), null, null));
            historico = SaldoDoMes.historico(mes, hoje, saldoHoje, desdeOInicioDoMes);
        }
        if (mes.equals(YearMonth.from(hoje))) {
            previsao = previsao(usuarioId, hoje, saldoHoje);
        }
        // Último dia do mês: o "fim do mês" é hoje. Sem previsão (pouco histórico): não mostra nada.
        BigDecimal saldoPrevisto = null;
        if (!previsao.isEmpty()) {
            saldoPrevisto = previsao.get(previsao.size() - 1).balance();
        } else if (hoje.equals(mes.atEndOfMonth())) {
            saldoPrevisto = saldoHoje;
        }

        PlanDto plano = planoService.paraDashboard(perfil, usuarioId);
        DashboardDto.Plan cartaoPlano = plano == null || plano.debtFreeDate() == null
                ? null
                : new DashboardDto.Plan(plano.debtFreeDate(), plano.progressPercent());

        return new DashboardDto(
                perfil.usuario().getNome(),
                saldoHoje,
                somar(totais, TipoMovimento.ENTRADA),
                somar(totais, TipoMovimento.SAIDA),
                saldoPrevisto,
                historico,
                previsao,
                gastosPorCategoria(totais),
                proximosPagamentos(dividas),
                alertaDeJuros(dividas),
                new DashboardDto.Debts(dividas.size(), perfil.totalDividas(),
                        (int) dividas.stream().filter(d -> DividaFront.JUROS_ALTOS.equals(DividaFront.status(d))).count()),
                cartaoPlano,
                trilhaService.resumo(usuarioId));
    }

    private List<BalancePoint> previsao(Long usuarioId, LocalDate hoje, BigDecimal saldoHoje) {
        LocalDate inicio = hoje.minusDays(SaldoDoMes.DIAS_DA_MEDIA - 1L);
        // Depósito e resgate do cofre não são gasto nem renda: o dinheiro só mudou de lugar.
        List<Transacao> recentes = transacaoDAO.listar(
                new FiltroTransacao(usuarioId, null, inicio, hoje.plusDays(1), null, null)).stream()
                .filter(transacao -> !CategoriaPadrao.DEPOSITO_COFRE.equals(transacao.getCategoriaNome())
                        && !CategoriaPadrao.RESGATE_COFRE.equals(transacao.getCategoriaNome()))
                .toList();
        if (recentes.isEmpty()) {
            return List.of(); // sem histórico não há o que prever
        }
        // Quem começou a usar há pouco: divide pelos dias que existem de verdade, não por 90.
        LocalDate primeira = recentes.stream().map(Transacao::getData).min(Comparator.naturalOrder()).orElse(inicio);
        int dias = (int) ChronoUnit.DAYS.between(primeira, hoje) + 1;
        // Com poucos dias a média engana (um salário de ontem viraria "+R$ 2.500 por dia").
        if (dias < SaldoDoMes.DIAS_MINIMOS_PARA_PREVER) {
            return List.of();
        }
        return SaldoDoMes.previsao(hoje, saldoHoje, recentes, dias);
    }

    /**
     * Depósito e resgate do cofre não entram em entradas/saídas: o dinheiro só mudou de lugar
     * (mesmo critério da AnaliseFinanceira).
     */
    static boolean movimentoDeCofre(TotalCategoria total) {
        return total.categoria().equals(CategoriaPadrao.DEPOSITO_COFRE)
                || total.categoria().equals(CategoriaPadrao.RESGATE_COFRE);
    }

    static BigDecimal somar(List<TotalCategoria> totais, TipoMovimento tipo) {
        return totais.stream()
                .filter(total -> total.tipo() == tipo && !movimentoDeCofre(total))
                .map(TotalCategoria::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Saídas agrupadas pelos códigos do front, da maior para a menor. */
    static List<CategoryExpense> gastosPorCategoria(List<TotalCategoria> totais) {
        Map<String, BigDecimal> porCodigo = new LinkedHashMap<>();
        for (TotalCategoria total : totais) {
            if (total.tipo() == TipoMovimento.SAIDA && !movimentoDeCofre(total)) {
                porCodigo.merge(CategoriaFront.codigo(total.categoria()), total.total(), BigDecimal::add);
            }
        }
        List<CategoryExpense> gastos = new ArrayList<>();
        porCodigo.forEach((codigo, valor) -> gastos.add(new CategoryExpense(codigo, valor)));
        gastos.sort((a, b) -> b.total().compareTo(a.total()));
        return gastos;
    }

    /** As próximas parcelas das dívidas ativas (atrasadas primeiro, pela data). */
    static List<UpcomingPayment> proximosPagamentos(List<Divida> dividas) {
        return dividas.stream()
                .filter(divida -> divida.getDataProximoVencimento() != null)
                .sorted(Comparator.comparing(Divida::getDataProximoVencimento))
                .limit(MAXIMO_DE_PAGAMENTOS)
                .map(divida -> new UpcomingPayment(
                        divida.getId(),
                        divida.getId(),
                        DividaFront.nome(divida),
                        DividaFront.parcelada(divida)
                                ? "Parcela " + Math.min(divida.getParcelasPagas() + 1, divida.getParcelasTotal())
                                        + " de " + divida.getParcelasTotal()
                                : "Pagamento mínimo",
                        divida.getDataProximoVencimento(),
                        divida.getValorParcela()))
                .toList();
    }

    /** A dívida que mais passa da média do mercado (nulo se nenhuma passa). */
    static InterestAlert alertaDeJuros(List<Divida> dividas) {
        return dividas.stream()
                .filter(divida -> DividaFront.acimaDoMercado(divida).signum() > 0)
                .max(Comparator.comparing(DividaFront::acimaDoMercado))
                .map(divida -> new InterestAlert(divida.getId(), DividaFront.nome(divida),
                        divida.getTaxaJurosMensal(), DividaFront.taxaDeMercado(divida.getTipo())))
                .orElse(null);
    }
}
