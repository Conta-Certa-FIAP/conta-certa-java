package br.com.fiap.contacerta.dao;

import java.math.BigDecimal;

import br.com.fiap.contacerta.model.TipoMovimento;

/** Resultado do SELECT com GROUP BY: quanto o usuário movimentou em uma categoria num período. */
public record TotalCategoria(
        Long categoriaId,
        String categoria,
        TipoMovimento tipo,
        boolean essencial,
        BigDecimal total) {
}
