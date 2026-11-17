package br.com.fiap.contacerta.model;

import java.math.BigDecimal;

/**
 * Espelha a tabela tb_plano_quitacao: o que a pessoa escolheu na tela "Meu plano".
 * Cada usuário tem no máximo um plano (a chave primária é o próprio cd_usuario).
 *
 * @param usuarioId   cd_usuario
 * @param valorMensal vl_mensal  (quanto ela pode pagar de dívidas por mês)
 * @param metodo      tp_metodo  ("AVALANCHE" ou "BOLA_DE_NEVE")
 */
public record PlanoSalvo(Long usuarioId, BigDecimal valorMensal, String metodo) {
}
