package br.com.fiap.contacerta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import br.com.fiap.contacerta.model.CategoriaPadrao;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.MeioPagamento;
import br.com.fiap.contacerta.model.StatusDivida;
import br.com.fiap.contacerta.model.TipoMovimento;
import br.com.fiap.contacerta.service.GeradorDadosSimulados.DadosSimulados;
import br.com.fiap.contacerta.service.GeradorDadosSimulados.TransacaoSimulada;

/**
 * Testa o gerador com várias "sementes" do Random: com a mesma semente o sorteio é sempre
 * igual, então o teste é repetível. As regras precisam valer em TODAS as sementes.
 */
class GeradorDadosSimuladosTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 11);
    private static final int SEMENTES = 300;

    private final GeradorDadosSimulados gerador = new GeradorDadosSimulados();

    private DadosSimulados gerar(long semente, BigDecimal renda) {
        return gerador.gerar(renda, "Nubank", HOJE, new Random(semente));
    }

    @Test
    void geraUmSalarioPorMesNosTresUltimosMeses() {
        for (long semente = 0; semente < SEMENTES; semente++) {
            List<TransacaoSimulada> salarios = gerar(semente, new BigDecimal("2800")).transacoes().stream()
                    .filter(t -> t.categoria().equals(CategoriaPadrao.SALARIO)).toList();
            assertEquals(3, salarios.size());
            salarios.forEach(s -> assertEquals(new BigDecimal("2800.00"), s.valor()));
        }
    }

    @Test
    void semRendaDeclaradaSorteiaEntre1500E4500() {
        for (long semente = 0; semente < SEMENTES; semente++) {
            BigDecimal salario = gerar(semente, null).transacoes().stream()
                    .filter(t -> t.categoria().equals(CategoriaPadrao.SALARIO)).findFirst().orElseThrow().valor();
            assertTrue(salario.doubleValue() >= 1500 && salario.doubleValue() <= 4500, "salário " + salario);
        }
    }

    @Test
    void geraDe30A45SaidasDentroDoPeriodo() {
        for (long semente = 0; semente < SEMENTES; semente++) {
            List<TransacaoSimulada> transacoes = gerar(semente, null).transacoes();
            long saidas = transacoes.stream().filter(t -> t.tipo() == TipoMovimento.SAIDA).count();
            assertTrue(saidas >= 30 && saidas <= 45, "semente " + semente + " gerou " + saidas + " saídas");
            for (TransacaoSimulada t : transacoes) {
                assertFalse(t.data().isAfter(HOJE), "transação no futuro");
                assertFalse(t.data().isBefore(HOJE.minusMonths(3)), "transação com mais de 3 meses");
                assertTrue(t.valor().signum() > 0, "valor deve ser positivo");
            }
        }
    }

    @Test
    void saldoNuncaFicaNegativoEBateComOExtrato() {
        for (long semente = 0; semente < SEMENTES; semente++) {
            DadosSimulados dados = gerar(semente, null);
            // refaz a conta do extrato em ordem de data (entradas primeiro no mesmo dia)
            List<TransacaoSimulada> ordenadas = dados.transacoes().stream()
                    .sorted(Comparator.comparing(TransacaoSimulada::data)
                            .thenComparing(t -> t.tipo() == TipoMovimento.ENTRADA ? 0 : 1))
                    .toList();
            BigDecimal saldo = BigDecimal.ZERO;
            for (TransacaoSimulada t : ordenadas) {
                if (t.tipo() == TipoMovimento.ENTRADA) {
                    saldo = saldo.add(t.valor());
                } else if (t.meio() != MeioPagamento.CARTAO_CREDITO) {
                    saldo = saldo.subtract(t.valor());
                }
                assertTrue(saldo.signum() >= 0, "saldo negativo na semente " + semente);
            }
            assertEquals(0, saldo.compareTo(dados.saldoFinal()), "saldo final diferente do extrato");
        }
    }

    @Test
    void faturaEhASomaDasComprasNoCreditoERespeitaOLimite() {
        for (long semente = 0; semente < SEMENTES; semente++) {
            DadosSimulados dados = gerar(semente, null);
            BigDecimal compras = dados.transacoes().stream()
                    .filter(t -> t.meio() == MeioPagamento.CARTAO_CREDITO)
                    .map(TransacaoSimulada::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal limite = dados.cartaoCredito().getLimite();

            assertEquals(0, compras.compareTo(dados.cartaoCredito().getFaturaAtual()));
            assertTrue(compras.compareTo(limite) <= 0, "fatura maior que o limite");
            assertTrue(limite.doubleValue() >= 800 && limite.doubleValue() <= 5000, "limite " + limite);
            int vencimento = dados.cartaoCredito().getDiaVencimento();
            assertTrue(vencimento >= 1 && vencimento <= 28);
            assertTrue(dados.cartaoCredito().getNumeroFinal().matches("\\d{4}"));
            assertTrue(dados.cartaoDebito().getNumeroFinal().matches("\\d{4}"));
        }
    }

    @Test
    void dividasSaoCoerentesComAsRegrasDoBanco() {
        for (long semente = 0; semente < SEMENTES; semente++) {
            List<Divida> dividas = gerar(semente, null).dividas();
            assertTrue(dividas.size() <= 3);
            assertEquals(dividas.size(), dividas.stream().map(Divida::getTipo).distinct().count(), "tipos repetidos");

            for (Divida divida : dividas) {
                assertTrue(divida.getValorOriginal().signum() > 0);
                assertTrue(divida.getSaldoDevedor().signum() > 0);
                assertTrue(divida.getValorParcela().signum() > 0);
                assertTrue(divida.getParcelasPagas() >= 0 && divida.getParcelasPagas() < divida.getParcelasTotal());
                double taxa = divida.getTaxaJurosMensal().doubleValue();
                assertTrue(taxa >= 0.8 && taxa <= 15.0, "taxa fora da faixa: " + taxa);

                // dias de atraso coerentes com o status
                if (divida.getStatus() == StatusDivida.ATRASADA) {
                    assertTrue(divida.getDiasAtraso() > 0);
                    assertTrue(divida.getDataProximoVencimento().isBefore(HOJE));
                } else {
                    assertEquals(StatusDivida.EM_DIA, divida.getStatus());
                    assertEquals(0, divida.getDiasAtraso());
                    assertTrue(divida.getDataProximoVencimento().isAfter(HOJE));
                }
                // a parcela precisa pagar mais que os juros do mês, senão a dívida nunca acabaria
                BigDecimal juros = CalculadoraFinanceira.jurosDoMes(divida.getSaldoDevedor(), divida.getTaxaJurosMensal());
                assertTrue(divida.getValorParcela().compareTo(juros) > 0, "parcela não cobre os juros");
            }
        }
    }

    @Test
    void datasDosSalariosSaoOsTresUltimosDias5() {
        assertEquals(List.of(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 9, 5)),
                GeradorDadosSimulados.datasDosSalarios(LocalDate.of(2026, 11, 11)));
        // antes do dia 5, o último salário foi no mês anterior
        assertEquals(List.of(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 9, 5), LocalDate.of(2026, 8, 5)),
                GeradorDadosSimulados.datasDosSalarios(LocalDate.of(2026, 11, 3)));
    }
}
