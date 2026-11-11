package br.com.fiap.contacerta.dto.conta;

/** Quantos registros simulados foram criados ao conectar o banco (para o front mostrar um aviso). */
public record ImportacaoResumo(int cartoes, int transacoes, int dividas) {
}
