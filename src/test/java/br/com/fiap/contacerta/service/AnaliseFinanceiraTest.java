package br.com.fiap.contacerta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.fiap.contacerta.dao.TotalCategoria;
import br.com.fiap.contacerta.model.CategoriaPadrao;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.SituacaoFinanceira;
import br.com.fiap.contacerta.model.StatusDivida;
import br.com.fiap.contacerta.model.TipoDivida;
import br.com.fiap.contacerta.model.TipoMovimento;
import br.com.fiap.contacerta.service.AnaliseFinanceira.NotaSaude;

/** Regras da situação financeira e da nota de saúde (sem banco de dados). */
class AnaliseFinanceiraTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 11);

    private static TotalCategoria total(String categoria, TipoMovimento tipo, boolean essencial, String valor) {
        return new TotalCategoria(1L, categoria, tipo, essencial, new BigDecimal(valor));
    }

    private static Divida divida(TipoDivida tipo, int diasAtraso) {
        Divida divida = new Divida();
        divida.setTipo(tipo);
        divida.setSaldoDevedor(new BigDecimal("1000.00"));
        divida.setValorParcela(new BigDecimal("100.00"));
        divida.setTaxaJurosMensal(new BigDecimal("5"));
        divida.setDiasAtraso(diasAtraso);
        divida.setStatus(diasAtraso > 0 ? StatusDivida.ATRASADA : StatusDivida.EM_DIA);
        return divida;
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    // ---------------------------------------------------------------- médias

    @Test
    void rendaMediaIgnoraResgateDoCofreETransferenciaRecebida() {
        List<TotalCategoria> totais = List.of(
                total(CategoriaPadrao.SALARIO, TipoMovimento.ENTRADA, false, "9000"),
                total(CategoriaPadrao.RENDA_EXTRA, TipoMovimento.ENTRADA, false, "600"),
                total(CategoriaPadrao.RESGATE_COFRE, TipoMovimento.ENTRADA, false, "5000"),
                total(CategoriaPadrao.TRANSFERENCIA_RECEBIDA, TipoMovimento.ENTRADA, false, "1000"),
                total(CategoriaPadrao.MERCADO, TipoMovimento.SAIDA, true, "3000"));
        assertEquals(bd("3200.00"), AnaliseFinanceira.rendaMedia(totais, 3));
    }

    @Test
    void gastosEssenciaisIgnoramPagamentoDeDividas() {
        List<TotalCategoria> totais = List.of(
                total(CategoriaPadrao.MERCADO, TipoMovimento.SAIDA, true, "1500"),
                total(CategoriaPadrao.MORADIA, TipoMovimento.SAIDA, true, "3000"),
                total(CategoriaPadrao.PAGAMENTO_DIVIDAS, TipoMovimento.SAIDA, true, "2000"),
                total(CategoriaPadrao.LAZER, TipoMovimento.SAIDA, false, "900"));
        assertEquals(bd("1500.00"), AnaliseFinanceira.gastosEssenciaisMedia(totais, 3));
    }

    @Test
    void naoEssenciaisVemOrdenadosEIgnoramDepositoNoCofre() {
        List<TotalCategoria> totais = List.of(
                total(CategoriaPadrao.LAZER, TipoMovimento.SAIDA, false, "100"),
                total(CategoriaPadrao.DELIVERY, TipoMovimento.SAIDA, false, "400"),
                total(CategoriaPadrao.DEPOSITO_COFRE, TipoMovimento.SAIDA, false, "900"),
                total(CategoriaPadrao.MERCADO, TipoMovimento.SAIDA, true, "800"));
        List<TotalCategoria> resultado = AnaliseFinanceira.naoEssenciais(totais);
        assertEquals(2, resultado.size());
        assertEquals(CategoriaPadrao.DELIVERY, resultado.get(0).categoria());
        assertEquals(CategoriaPadrao.LAZER, resultado.get(1).categoria());
    }

    @Test
    void mesesDeHistoricoVaiDe1A3() {
        assertEquals(3, AnaliseFinanceira.mesesDeHistorico(null, HOJE));
        assertEquals(1, AnaliseFinanceira.mesesDeHistorico(HOJE, HOJE));
        assertEquals(1, AnaliseFinanceira.mesesDeHistorico(HOJE.minusDays(29), HOJE));
        assertEquals(2, AnaliseFinanceira.mesesDeHistorico(HOJE.minusDays(30), HOJE));
        assertEquals(3, AnaliseFinanceira.mesesDeHistorico(HOJE.minusDays(67), HOJE));
        assertEquals(3, AnaliseFinanceira.mesesDeHistorico(HOJE.minusDays(200), HOJE));
    }

    @Test
    void comprometimento() {
        assertEquals(bd("25.00"), AnaliseFinanceira.comprometimento(bd("500"), bd("2000")));
        assertNull(AnaliseFinanceira.comprometimento(bd("500"), null));
    }

    // ---------------------------------------------------------------- situação

    @Test
    void confortavelAte30PorCentoSemAtraso() {
        assertEquals(SituacaoFinanceira.CONFORTAVEL, AnaliseFinanceira.classificar(bd("30.00"),
                List.of(divida(TipoDivida.EMPRESTIMO_PESSOAL, 0))));
        assertEquals(SituacaoFinanceira.CONFORTAVEL, AnaliseFinanceira.classificar(null, List.of()));
    }

    @Test
    void atencaoEntre30E50OuComRotativo() {
        assertEquals(SituacaoFinanceira.ATENCAO, AnaliseFinanceira.classificar(bd("30.01"),
                List.of(divida(TipoDivida.EMPRESTIMO_PESSOAL, 0))));
        assertEquals(SituacaoFinanceira.ATENCAO, AnaliseFinanceira.classificar(bd("50.00"),
                List.of(divida(TipoDivida.CONSIGNADO, 0))));
        assertEquals(SituacaoFinanceira.ATENCAO, AnaliseFinanceira.classificar(bd("5.00"),
                List.of(divida(TipoDivida.CARTAO_ROTATIVO, 0))));
        assertEquals(SituacaoFinanceira.ATENCAO, AnaliseFinanceira.classificar(bd("5.00"),
                List.of(divida(TipoDivida.CHEQUE_ESPECIAL, 0))));
    }

    @Test
    void emRiscoAcimaDe50OuComAtrasoAte90Dias() {
        assertEquals(SituacaoFinanceira.EM_RISCO, AnaliseFinanceira.classificar(bd("50.01"),
                List.of(divida(TipoDivida.CONSIGNADO, 0))));
        assertEquals(SituacaoFinanceira.EM_RISCO, AnaliseFinanceira.classificar(bd("10.00"),
                List.of(divida(TipoDivida.CREDIARIO, 90))));
    }

    @Test
    void inadimplenteComAtrasoAcimaDe90Dias() {
        assertEquals(SituacaoFinanceira.INADIMPLENTE, AnaliseFinanceira.classificar(bd("10.00"),
                List.of(divida(TipoDivida.CREDIARIO, 0), divida(TipoDivida.CREDIARIO, 91))));
    }

    @Test
    void valeASituacaoMaisGrave() {
        // rotativo (ATENCAO) + comprometimento de 60% (EM_RISCO) + atraso de 120 dias (INADIMPLENTE)
        assertEquals(SituacaoFinanceira.INADIMPLENTE, AnaliseFinanceira.classificar(bd("60.00"),
                List.of(divida(TipoDivida.CARTAO_ROTATIVO, 120))));
    }

    // ---------------------------------------------------------------- nota

    @Test
    void descontoLinearDoComprometimento() {
        assertEquals(0, AnaliseFinanceira.descontoPorComprometimento(0));
        assertEquals(0, AnaliseFinanceira.descontoPorComprometimento(30));
        assertEquals(20, AnaliseFinanceira.descontoPorComprometimento(55)); // metade do caminho
        assertEquals(40, AnaliseFinanceira.descontoPorComprometimento(80));
        assertEquals(40, AnaliseFinanceira.descontoPorComprometimento(150));
    }

    @Test
    void notaMaximaComBonusFicaEm100() {
        NotaSaude nota = AnaliseFinanceira.calcularNota(bd("10.00"), List.of(), bd("3000"),
                bd("300"), bd("1000"), bd("3000"));
        assertEquals(100, nota.nota()); // 100 + 5 de bônus, limitado a 100
        assertEquals(1, nota.itens().size());
    }

    @Test
    void notaSomaTodosOsDescontos() {
        // comprometimento 55% (-20), atraso até 90 dias (-25), rotativo (-10),
        // não essenciais 1000 > 30% de 3000 (-10), saldo 100 < 10% de 3000 (-5) => 30
        NotaSaude nota = AnaliseFinanceira.calcularNota(bd("55.00"),
                List.of(divida(TipoDivida.CARTAO_ROTATIVO, 30)), bd("3000"), bd("1000"), bd("100"), bd("0"));
        assertEquals(30, nota.nota());
        assertEquals(5, nota.itens().size());
    }

    @Test
    void atrasoGraveDesconta40ENaoSomaComOde25() {
        NotaSaude nota = AnaliseFinanceira.calcularNota(bd("10.00"),
                List.of(divida(TipoDivida.CREDIARIO, 30), divida(TipoDivida.CREDIARIO, 100)),
                bd("3000"), bd("0"), bd("1000"), bd("0"));
        assertEquals(60, nota.nota());
    }

    @Test
    void notaNuncaFicaAbaixoDeZero() {
        // 100 - 40 (comprometimento) - 40 (atraso grave) - 10 (rotativo) - 10 - 5 = -5 -> limita em 0
        NotaSaude nota = AnaliseFinanceira.calcularNota(bd("200.00"),
                List.of(divida(TipoDivida.CARTAO_ROTATIVO, 200)), bd("1000"), bd("900"), bd("0"), bd("0"));
        assertEquals(0, nota.nota());
    }

    @Test
    void semRendaPulaOsItensQueDependemDela() {
        NotaSaude nota = AnaliseFinanceira.calcularNota(null, List.of(divida(TipoDivida.CREDIARIO, 10)),
                null, bd("500"), bd("0"), bd("0"));
        assertEquals(75, nota.nota()); // só o desconto do atraso
    }
}
