package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.client.BancoCentralClient;
import br.com.fiap.contacerta.client.DadoSerie;
import br.com.fiap.contacerta.dao.IndicadorEconomicoDAO;
import br.com.fiap.contacerta.dto.indicador.HistoricoResponse;
import br.com.fiap.contacerta.dto.indicador.HistoricoResponse.PontoHistorico;
import br.com.fiap.contacerta.dto.indicador.HistoricoResponse.SerieHistorico;
import br.com.fiap.contacerta.dto.indicador.IndicadoresResponse;
import br.com.fiap.contacerta.dto.indicador.IndicadoresResponse.Indicador;
import br.com.fiap.contacerta.exception.BancoCentralIndisponivelException;
import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;
import br.com.fiap.contacerta.exception.ServicoIndisponivelException;
import br.com.fiap.contacerta.model.IndicadorEconomico;
import br.com.fiap.contacerta.model.SerieBcb;

/**
 * "Situação do país": Selic, IPCA e dólar com dados reais do Banco Central.
 *
 * Para não chamar o Banco Central a cada requisição, os valores ficam guardados na tabela
 * tb_indicador_economico (cache):
 *  - se a última busca tem MENOS de 12 horas, respondemos direto do banco;
 *  - senão, buscamos no Banco Central, salvamos e respondemos;
 *  - se o Banco Central falhar, respondemos com o último dado salvo e "desatualizado": true;
 *  - se falhar e não houver nada salvo, respondemos 503.
 */
@Service
public class IndicadorService {

    private static final Logger log = LoggerFactory.getLogger(IndicadorService.class);

    static final Duration VALIDADE_DO_CACHE = Duration.ofHours(12);
    /** Buscamos 25 meses para o histórico de até 24 meses ter sempre o mês anterior para comparar. */
    static final int MESES_BUSCADOS = 25;
    static final int MAXIMO_DE_MESES_NO_HISTORICO = 24;

    private static final String FONTE = "Banco Central do Brasil - SGS (api.bcb.gov.br)";
    private static final String JURO_REAL_EXPLICACAO = "Juro real = (1 + Selic) / (1 + IPCA de 12 meses) - 1. "
            + "É quanto a taxa básica de juros rende acima da inflação.";

    private final IndicadorEconomicoDAO indicadorDAO;
    private final BancoCentralClient bancoCentral;

    public IndicadorService(IndicadorEconomicoDAO indicadorDAO, BancoCentralClient bancoCentral) {
        this.indicadorDAO = indicadorDAO;
        this.bancoCentral = bancoCentral;
    }

    /** GET /indicadores: o último valor de cada indicador e o juro real. */
    public IndicadoresResponse indicadores() {
        boolean desatualizado = atualizarCache();

        IndicadorEconomico selic = maisRecente(SerieBcb.SELIC);
        IndicadorEconomico ipcaMensal = maisRecente(SerieBcb.IPCA_MENSAL);
        IndicadorEconomico ipca12 = maisRecente(SerieBcb.IPCA_12_MESES);
        IndicadorEconomico dolar = maisRecente(SerieBcb.DOLAR);

        // "Atualizado em" = a busca mais antiga entre os quatro indicadores.
        LocalDateTime atualizadoEm = selic.getDataAtualizacao();
        for (IndicadorEconomico indicador : List.of(ipcaMensal, ipca12, dolar)) {
            if (indicador.getDataAtualizacao().isBefore(atualizadoEm)) {
                atualizadoEm = indicador.getDataAtualizacao();
            }
        }

        return new IndicadoresResponse(
                paraIndicador(SerieBcb.SELIC, selic),
                paraIndicador(SerieBcb.IPCA_MENSAL, ipcaMensal),
                paraIndicador(SerieBcb.IPCA_12_MESES, ipca12),
                paraIndicador(SerieBcb.DOLAR, dolar),
                juroReal(selic.getValor(), ipca12.getValor()),
                JURO_REAL_EXPLICACAO,
                atualizadoEm,
                desatualizado,
                FONTE);
    }

    /** GET /indicadores/historico: um ponto por mês para cada indicador, com a variação mensal. */
    public HistoricoResponse historico(int meses) {
        if (meses < 1 || meses > MAXIMO_DE_MESES_NO_HISTORICO) {
            throw new RequisicaoInvalidaException(
                    "O parâmetro 'meses' deve estar entre 1 e " + MAXIMO_DE_MESES_NO_HISTORICO + ".");
        }
        boolean desatualizado = atualizarCache();
        // Lemos alguns meses a mais: o IPCA sai com atraso e o 1º ponto precisa do mês anterior.
        LocalDate inicio = YearMonth.now().minusMonths(meses + 3L).atDay(1);

        List<SerieHistorico> series = new ArrayList<>();
        for (SerieBcb serie : SerieBcb.values()) {
            List<DadoSerie> dados = indicadorDAO.listarDesde(serie.getCodigo(), inicio).stream()
                    .map(indicador -> new DadoSerie(indicador.getDataReferencia(), indicador.getValor()))
                    .toList();
            series.add(new SerieHistorico(serie.name(), serie.getCodigo(), serie.getNome(), serie.getUnidade(),
                    montarPontos(ultimoDeCadaMes(dados), meses)));
        }
        return new HistoricoResponse(meses, series, desatualizado, FONTE);
    }

    // ------------------------------------------------------------------ cache

    /**
     * Garante que as quatro séries estão em dia. Devolve true se alguma ficou desatualizada
     * (Banco Central fora do ar, mas havia dado salvo).
     */
    private boolean atualizarCache() {
        LocalDateTime agora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDate hoje = agora.toLocalDate();
        boolean bancoCentralFora = false;
        boolean desatualizado = false;

        for (SerieBcb serie : SerieBcb.values()) {
            Optional<LocalDateTime> ultimaBusca = indicadorDAO.ultimaAtualizacao(serie.getCodigo());
            if (ultimaBusca.isPresent() && cacheValido(ultimaBusca.get(), agora)) {
                continue; // menos de 12 horas: usa o que está no banco
            }

            // Se o Banco Central já falhou nesta requisição, não insistimos nas outras séries
            // (cada tentativa pode demorar até 10 segundos).
            if (!bancoCentralFora) {
                try {
                    LocalDate inicio = hoje.minusMonths(MESES_BUSCADOS).withDayOfMonth(1);
                    List<DadoSerie> dados = bancoCentral.buscarPeriodo(serie.getCodigo(), inicio, hoje);
                    indicadorDAO.salvar(paraSalvar(serie, ultimoDeCadaMes(dados), agora));
                    continue;
                } catch (BancoCentralIndisponivelException e) {
                    log.warn("Banco Central indisponível para a série {}: {}", serie.getCodigo(), e.getMessage());
                    bancoCentralFora = true;
                }
            }

            if (ultimaBusca.isEmpty()) {
                throw new ServicoIndisponivelException("O Banco Central está fora do ar e ainda não temos "
                        + "indicadores salvos. Tente novamente em alguns minutos.");
            }
            desatualizado = true;
        }
        return desatualizado;
    }

    static boolean cacheValido(LocalDateTime ultimaBusca, LocalDateTime agora) {
        return ultimaBusca.isAfter(agora.minus(VALIDADE_DO_CACHE));
    }

    private List<IndicadorEconomico> paraSalvar(SerieBcb serie, List<DadoSerie> dados, LocalDateTime agora) {
        return dados.stream()
                .map(dado -> new IndicadorEconomico(serie.getCodigo(), serie.getNome(), dado.data(), dado.valor(), agora))
                .toList();
    }

    private IndicadorEconomico maisRecente(SerieBcb serie) {
        return indicadorDAO.buscarMaisRecente(serie.getCodigo())
                .orElseThrow(() -> new ServicoIndisponivelException(
                        "Indicador '" + serie.getNome() + "' indisponível no momento. Tente novamente em alguns minutos."));
    }

    private Indicador paraIndicador(SerieBcb serie, IndicadorEconomico indicador) {
        return new Indicador(serie.name(), serie.getCodigo(), serie.getNome(), serie.getUnidade(),
                indicador.getValor(), indicador.getDataReferencia());
    }

    // ------------------------------------------------------------------ cálculos (estáticos e testáveis)

    /** Juro real em %: (1 + Selic) / (1 + IPCA 12 meses) - 1. */
    static BigDecimal juroReal(BigDecimal selicAnual, BigDecimal ipca12Meses) {
        double selic = selicAnual.doubleValue() / 100.0;
        double ipca = ipca12Meses.doubleValue() / 100.0;
        return CalculadoraFinanceira.arredondar(((1 + selic) / (1 + ipca) - 1) * 100);
    }

    /**
     * Agrega por mês ficando com o ÚLTIMO valor de cada mês (séries diárias como dólar e Selic
     * têm um valor por dia). O TreeMap mantém os meses em ordem.
     */
    static List<DadoSerie> ultimoDeCadaMes(List<DadoSerie> dados) {
        Map<YearMonth, DadoSerie> porMes = new TreeMap<>();
        for (DadoSerie dado : dados) {
            YearMonth mes = YearMonth.from(dado.data());
            DadoSerie atual = porMes.get(mes);
            if (atual == null || dado.data().isAfter(atual.data())) {
                porMes.put(mes, dado);
            }
        }
        return new ArrayList<>(porMes.values());
    }

    /** Monta os pontos do gráfico (os últimos "meses" pontos), comparando cada mês com o anterior. */
    static List<PontoHistorico> montarPontos(List<DadoSerie> ultimoPorMes, int meses) {
        List<PontoHistorico> pontos = new ArrayList<>();
        for (int i = 0; i < ultimoPorMes.size(); i++) {
            DadoSerie dado = ultimoPorMes.get(i);
            BigDecimal diferenca = i == 0 ? null : dado.valor().subtract(ultimoPorMes.get(i - 1).valor());
            pontos.add(new PontoHistorico(YearMonth.from(dado.data()).toString(), dado.valor(),
                    diferenca, variacao(diferenca)));
        }
        int sobrando = Math.max(0, pontos.size() - meses);
        return pontos.subList(sobrando, pontos.size());
    }

    static String variacao(BigDecimal diferenca) {
        if (diferenca == null) {
            return null;
        }
        if (diferenca.signum() > 0) {
            return "subiu";
        }
        return diferenca.signum() < 0 ? "caiu" : "estável";
    }
}
