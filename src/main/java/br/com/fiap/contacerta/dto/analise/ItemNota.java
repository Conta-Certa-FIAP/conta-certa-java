package br.com.fiap.contacerta.dto.analise;

/** Um motivo que somou ou tirou pontos da nota de saúde financeira (ex.: "Dívida em atraso", -25). */
public record ItemNota(String motivo, int pontos) {
}
