package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.util.List;

import br.com.fiap.contacerta.dao.TotalCategoria;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.Usuario;

/**
 * "Retrato" financeiro do usuário, montado uma vez pelo PerfilFinanceiroService e reaproveitado
 * por todas as análises (situação, nota, consultoria, resumo, custo em horas).
 *
 * rendaMedia e origemRenda ficam nulos quando não há entradas no extrato nem renda declarada.
 */
public record PerfilFinanceiro(
        Usuario usuario,
        BigDecimal rendaMedia,
        String origemRenda,                            // "EXTRATO" ou "DECLARADA"
        BigDecimal gastosEssenciaisMedia,
        List<TotalCategoria> naoEssenciaisUltimoMes,   // da maior para a menor
        BigDecimal saldoContas,
        BigDecimal totalCofres,
        List<Divida> dividasAtivas) {

    public boolean temRenda() {
        return rendaMedia != null;
    }

    public BigDecimal gastosNaoEssenciaisUltimoMes() {
        return naoEssenciaisUltimoMes.stream().map(TotalCategoria::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Soma das parcelas mensais das dívidas não quitadas. */
    public BigDecimal totalParcelas() {
        return dividasAtivas.stream().map(Divida::getValorParcela).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Soma dos saldos devedores das dívidas não quitadas. */
    public BigDecimal totalDividas() {
        return dividasAtivas.stream().map(Divida::getSaldoDevedor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
