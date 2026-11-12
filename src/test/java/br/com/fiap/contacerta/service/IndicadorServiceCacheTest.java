package br.com.fiap.contacerta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.fiap.contacerta.client.BancoCentralClient;
import br.com.fiap.contacerta.client.DadoSerie;
import br.com.fiap.contacerta.dao.IndicadorEconomicoDAO;
import br.com.fiap.contacerta.dto.indicador.IndicadoresResponse;
import br.com.fiap.contacerta.exception.BancoCentralIndisponivelException;
import br.com.fiap.contacerta.exception.ServicoIndisponivelException;
import br.com.fiap.contacerta.model.IndicadorEconomico;
import br.com.fiap.contacerta.model.SerieBcb;

/**
 * Testa as regras do cache dos indicadores SEM banco de dados e SEM internet.
 * Para isso usamos "dublês": classes filhas do DAO e do cliente que guardam tudo em memória
 * e deixam o teste decidir se o Banco Central está no ar ou fora do ar.
 */
class IndicadorServiceCacheTest {

    /** DAO falso: guarda os indicadores em uma lista em vez de usar o Oracle. */
    private static class DaoEmMemoria extends IndicadorEconomicoDAO {
        final List<IndicadorEconomico> linhas = new ArrayList<>();
        int vezesQueSalvou = 0;

        DaoEmMemoria() {
            super(null); // não há DataSource: todos os métodos que usam o banco foram substituídos
        }

        @Override
        public Optional<LocalDateTime> ultimaAtualizacao(int codigoSerie) {
            return linhas.stream().filter(l -> l.getCodigoSerie() == codigoSerie)
                    .map(IndicadorEconomico::getDataAtualizacao).max(Comparator.naturalOrder());
        }

        @Override
        public void salvar(List<IndicadorEconomico> indicadores) {
            vezesQueSalvou++;
            linhas.addAll(indicadores);
        }

        @Override
        public Optional<IndicadorEconomico> buscarMaisRecente(int codigoSerie) {
            return linhas.stream().filter(l -> l.getCodigoSerie() == codigoSerie)
                    .max(Comparator.comparing(IndicadorEconomico::getDataReferencia));
        }

        void adicionar(SerieBcb serie, String valor, LocalDateTime atualizadoEm) {
            linhas.add(new IndicadorEconomico(serie.getCodigo(), serie.getNome(),
                    atualizadoEm.toLocalDate(), new BigDecimal(valor), atualizadoEm));
        }

        void preencherTudo(LocalDateTime atualizadoEm) {
            adicionar(SerieBcb.SELIC, "13.75", atualizadoEm);
            adicionar(SerieBcb.IPCA_MENSAL, "0.82", atualizadoEm);
            adicionar(SerieBcb.IPCA_12_MESES, "4.58", atualizadoEm);
            adicionar(SerieBcb.DOLAR, "5.56", atualizadoEm);
        }
    }

    /** Cliente falso: nunca acessa a internet. */
    private static class BancoCentralFalso extends BancoCentralClient {
        boolean foraDoAr = false;
        int chamadas = 0;

        BancoCentralFalso() {
            super(new ObjectMapper());
        }

        @Override
        public List<DadoSerie> buscarPeriodo(int codigoSerie, LocalDate inicio, LocalDate fim) {
            chamadas++;
            if (foraDoAr) {
                throw new BancoCentralIndisponivelException("fora do ar (teste)");
            }
            return List.of(new DadoSerie(fim.minusMonths(1), new BigDecimal("10.00")),
                    new DadoSerie(fim, new BigDecimal("11.00")));
        }
    }

    private final DaoEmMemoria dao = new DaoEmMemoria();
    private final BancoCentralFalso bancoCentral = new BancoCentralFalso();
    private final IndicadorService service = new IndicadorService(dao, bancoCentral);

    @Test
    void cacheComMenosDe12HorasNaoChamaOBancoCentral() {
        dao.preencherTudo(LocalDateTime.now().minusHours(2));

        IndicadoresResponse resposta = service.indicadores();

        assertEquals(0, bancoCentral.chamadas);
        assertFalse(resposta.desatualizado());
        assertEquals(new BigDecimal("13.75"), resposta.selic().valor());
        assertEquals(new BigDecimal("8.77"), resposta.juroReal());
    }

    @Test
    void cacheVencidoBuscaNoBancoCentralESalva() {
        dao.preencherTudo(LocalDateTime.now().minusDays(2));

        IndicadoresResponse resposta = service.indicadores();

        assertEquals(4, bancoCentral.chamadas); // uma busca por série
        assertEquals(4, dao.vezesQueSalvou);
        assertFalse(resposta.desatualizado());
        assertEquals(new BigDecimal("11.00"), resposta.selic().valor()); // valor novo do "Banco Central"
    }

    @Test
    void semNadaSalvoBuscaTudoNoBancoCentral() {
        IndicadoresResponse resposta = service.indicadores();

        assertEquals(4, bancoCentral.chamadas);
        assertFalse(resposta.desatualizado());
        assertEquals(new BigDecimal("11.00"), resposta.dolar().valor());
    }

    @Test
    void bancoCentralForaDoArDevolveUltimoDadoSalvoComoDesatualizado() {
        dao.preencherTudo(LocalDateTime.now().minusDays(2));
        bancoCentral.foraDoAr = true;

        IndicadoresResponse resposta = service.indicadores();

        assertTrue(resposta.desatualizado());
        assertEquals(new BigDecimal("13.75"), resposta.selic().valor()); // o valor antigo, salvo
        assertEquals(0, dao.vezesQueSalvou);
        // depois da primeira falha não insistimos nas outras séries (cada tentativa pode levar 10 s)
        assertEquals(1, bancoCentral.chamadas);
    }

    @Test
    void bancoCentralForaDoArESemNadaSalvoDa503() {
        bancoCentral.foraDoAr = true;
        assertThrows(ServicoIndisponivelException.class, service::indicadores);
    }
}
