package br.com.fiap.contacerta.dao;

import java.time.LocalDate;

import br.com.fiap.contacerta.model.TipoMovimento;

/**
 * Filtros da listagem de transações. Informe usuarioId (todas as contas do usuário) OU contaId.
 * Os demais são opcionais (null = não filtra). O período vale de "inicio" até ANTES de "fimExclusivo".
 */
public record FiltroTransacao(
        Long usuarioId,
        Long contaId,
        LocalDate inicio,
        LocalDate fimExclusivo,
        TipoMovimento tipo,
        Long categoriaId) {
}
