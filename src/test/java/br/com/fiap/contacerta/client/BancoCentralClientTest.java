package br.com.fiap.contacerta.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.fiap.contacerta.exception.BancoCentralIndisponivelException;

/**
 * Testa o cliente do Banco Central.
 *  - A leitura do JSON é testada com um texto fixo (não depende de internet).
 *  - A chamada REAL à API é feita de verdade; se não houver internet ou o Banco Central
 *    estiver fora do ar, o teste é PULADO (não falha), para o build não depender da rede.
 */
class BancoCentralClientTest {

    private final BancoCentralClient client = new BancoCentralClient(new ObjectMapper());

    @Test
    void converteOJsonDoBancoCentral() {
        String json = """
                [{"data":"10/11/2026","valor":"5.5840"},{"data":"11/11/2026","valor":"5.5606"}]
                """;
        List<DadoSerie> dados = client.converter(json);

        assertEquals(2, dados.size());
        assertEquals(LocalDate.of(2026, 11, 10), dados.get(0).data()); // dd/MM/yyyy -> LocalDate
        assertEquals(new BigDecimal("5.5840"), dados.get(0).valor()); // texto -> número
        assertEquals(LocalDate.of(2026, 11, 11), dados.get(1).data());
    }

    @Test
    void montaAUrlComAsDatasNoFormatoBrasileiro() {
        String url = client.montarUrl(432, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 11, 11));
        assertEquals("https://api.bcb.gov.br/dados/serie/bcdata.sgs.432/dados"
                + "?formato=json&dataInicial=05/01/2026&dataFinal=11/11/2026", url);
    }

    @Test
    void chamadaRealDaSelic() {
        LocalDate hoje = LocalDate.now();
        List<DadoSerie> dados;
        try {
            dados = client.buscarPeriodo(432, hoje.minusDays(40), hoje);
        } catch (BancoCentralIndisponivelException e) {
            Assumptions.abort("Banco Central inacessível neste momento: " + e.getMessage());
            return;
        }
        assertFalse(dados.isEmpty());
        for (DadoSerie dado : dados) {
            assertFalse(dado.data().isAfter(hoje));
            // a meta da Selic é um percentual ao ano: algo entre 0% e 100%
            assertTrue(dado.valor().doubleValue() > 0 && dado.valor().doubleValue() < 100, "Selic " + dado.valor());
        }
    }
}
