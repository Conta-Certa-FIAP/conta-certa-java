package br.com.fiap.contacerta.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.fiap.contacerta.exception.BancoCentralIndisponivelException;

/**
 * Cliente HTTP da API pública do Banco Central (SGS - Sistema Gerenciador de Séries Temporais).
 * Não precisa de chave. Cada indicador é uma "série" identificada por um código:
 *   432 = meta da Selic | 433 = IPCA mensal | 13522 = IPCA 12 meses | 1 = dólar comercial (venda)
 *
 * Resposta do Banco Central (repare: data em dd/MM/yyyy e valor como TEXTO):
 *   [{"data":"10/11/2026","valor":"5.5840"}, {"data":"11/11/2026","valor":"5.5606"}]
 *
 * Usamos a consulta por intervalo de datas porque a rota ".../ultimos/N" aceita no máximo N = 20,
 * o que não cobre 12 meses de uma série diária (conferido com requisições reais).
 */
@Component
public class BancoCentralClient {

    private static final String URL_BASE = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.";
    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(10);
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final HttpClient http;
    private final ObjectMapper json;

    public BancoCentralClient(ObjectMapper json) {
        this.json = json;
        // HttpClient é o cliente HTTP que já vem no Java (java.net.http), sem biblioteca extra.
        this.http = HttpClient.newBuilder().connectTimeout(TEMPO_LIMITE).build();
    }

    /** Busca os valores de uma série entre duas datas (as duas inclusive). */
    public List<DadoSerie> buscarPeriodo(int codigoSerie, LocalDate inicio, LocalDate fim) {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(montarUrl(codigoSerie, inicio, fim)))
                .timeout(TEMPO_LIMITE) // se o Banco Central não responder em 10 segundos, desistimos
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() != 200) {
                throw new BancoCentralIndisponivelException(
                        "Banco Central respondeu com status " + resposta.statusCode() + " para a série " + codigoSerie);
            }
            List<DadoSerie> dados = converter(resposta.body());
            if (dados.isEmpty()) {
                throw new BancoCentralIndisponivelException("Banco Central não devolveu dados da série " + codigoSerie);
            }
            return dados;
        } catch (IOException e) {
            throw new BancoCentralIndisponivelException("Falha ao consultar a série " + codigoSerie, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // boa prática: devolve o sinal de interrupção à thread
            throw new BancoCentralIndisponivelException("Consulta à série " + codigoSerie + " interrompida", e);
        }
    }

    String montarUrl(int codigoSerie, LocalDate inicio, LocalDate fim) {
        return URL_BASE + codigoSerie + "/dados?formato=json"
                + "&dataInicial=" + inicio.format(DATA_BR)
                + "&dataFinal=" + fim.format(DATA_BR);
    }

    /** Transforma o texto JSON em objetos Java: data dd/MM/yyyy -> LocalDate, valor texto -> BigDecimal. */
    List<DadoSerie> converter(String corpo) {
        try {
            JsonNode raiz = json.readTree(corpo);
            if (raiz == null || !raiz.isArray()) {
                throw new BancoCentralIndisponivelException("Resposta do Banco Central em formato inesperado");
            }
            List<DadoSerie> dados = new ArrayList<>();
            for (JsonNode item : raiz) {
                LocalDate data = LocalDate.parse(item.get("data").asText(), DATA_BR);
                BigDecimal valor = new BigDecimal(item.get("valor").asText().trim());
                dados.add(new DadoSerie(data, valor));
            }
            return dados;
        } catch (IOException | RuntimeException e) {
            if (e instanceof BancoCentralIndisponivelException conhecida) {
                throw conhecida;
            }
            throw new BancoCentralIndisponivelException("Não foi possível ler a resposta do Banco Central", e);
        }
    }
}
