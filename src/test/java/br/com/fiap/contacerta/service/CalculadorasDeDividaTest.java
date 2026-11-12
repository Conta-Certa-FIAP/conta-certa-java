package br.com.fiap.contacerta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.fiap.contacerta.dto.analise.CustoEmHorasResponse;
import br.com.fiap.contacerta.dto.analise.PlanoQuitacao;
import br.com.fiap.contacerta.dto.analise.ProjecaoDividaResponse;
import br.com.fiap.contacerta.dto.analise.ProjecaoDividaResponse.EncargosAtraso;
import br.com.fiap.contacerta.dto.analise.RenegociacaoResponse;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.StatusDivida;
import br.com.fiap.contacerta.model.TipoDivida;
import br.com.fiap.contacerta.service.PlanoQuitacaoCalculadora.Metodo;

/** Plano avalanche/bola de neve, projeção, renegociação e custo em horas (sem banco de dados). */
class CalculadorasDeDividaTest {

    private static Divida divida(long id, String credor, String saldo, String taxa, String parcela) {
        Divida divida = new Divida();
        divida.setId(id);
        divida.setCredor(credor);
        divida.setTipo(TipoDivida.EMPRESTIMO_PESSOAL);
        divida.setSaldoDevedor(new BigDecimal(saldo));
        divida.setTaxaJurosMensal(new BigDecimal(taxa));
        divida.setValorParcela(new BigDecimal(parcela));
        divida.setDiasAtraso(0);
        divida.setStatus(StatusDivida.EM_DIA);
        return divida;
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    // ---------------------------------------------------------------- plano de quitação

    /** "Cara" tem juros altos e saldo maior; "Pequena" tem juros baixos e saldo menor. */
    private static List<Divida> duasDividas() {
        return List.of(divida(1, "Cara", "1000.00", "10", "100.00"), divida(2, "Pequena", "500.00", "2", "50.00"));
    }

    @Test
    void avalancheComecaPelaMaiorTaxaEBolaDeNevePeloMenorSaldo() {
        PlanoQuitacao avalanche = PlanoQuitacaoCalculadora.simular(duasDividas(), bd("400.00"), Metodo.AVALANCHE);
        PlanoQuitacao bolaDeNeve = PlanoQuitacaoCalculadora.simular(duasDividas(), bd("400.00"), Metodo.BOLA_DE_NEVE);

        assertTrue(avalanche.quitaTudo());
        assertTrue(bolaDeNeve.quitaTudo());
        assertEquals("Cara", avalanche.ordemQuitacao().get(0).credor());
        assertEquals("Pequena", bolaDeNeve.ordemQuitacao().get(0).credor());
        assertEquals(2, avalanche.ordemQuitacao().size());
        assertEquals(5, avalanche.meses());
    }

    @Test
    void avalanchePagaMenosJurosQueBolaDeNeve() {
        PlanoQuitacao avalanche = PlanoQuitacaoCalculadora.simular(duasDividas(), bd("400.00"), Metodo.AVALANCHE);
        PlanoQuitacao bolaDeNeve = PlanoQuitacaoCalculadora.simular(duasDividas(), bd("400.00"), Metodo.BOLA_DE_NEVE);

        assertTrue(avalanche.totalJuros().compareTo(bolaDeNeve.totalJuros()) < 0);
        assertTrue(avalanche.totalPago().compareTo(bolaDeNeve.totalPago()) < 0);
    }

    @Test
    void totalPagoEhOSaldoInicialMaisOsJuros() {
        PlanoQuitacao plano = PlanoQuitacaoCalculadora.simular(duasDividas(), bd("400.00"), Metodo.AVALANCHE);
        assertEquals(0, plano.totalPago().compareTo(bd("1500.00").add(plano.totalJuros())));
        // a evolução termina com saldo zero
        assertEquals(0, plano.evolucao().get(plano.evolucao().size() - 1).saldoDevedorTotal().signum());
    }

    @Test
    void valorQueNaoCobreOsJurosNaoQuitaEAvisa() {
        List<Divida> dividas = List.of(divida(1, "Rotativo", "10000.00", "10", "100.00"));
        PlanoQuitacao plano = PlanoQuitacaoCalculadora.simular(dividas, bd("500.00"), Metodo.AVALANCHE);

        assertFalse(plano.quitaTudo());
        assertEquals(PlanoQuitacaoCalculadora.LIMITE_MESES, plano.meses());
        assertNotNull(plano.aviso());
        assertTrue(plano.ordemQuitacao().isEmpty());
    }

    // ---------------------------------------------------------------- projeção

    @Test
    void projecaoPagandoAParcelaZeraEmDozeMeses() {
        Divida divida = divida(1, "Loja", "1000.00", "2", "94.56"); // Price de 12x
        ProjecaoDividaResponse projecao = ProjecaoDividaCalculadora.projetar(divida, 24);

        int linhas = projecao.pagandoAParcela().size();
        assertTrue(linhas == 12 || linhas == 13, "linhas: " + linhas); // 13ª só se sobrarem centavos
        assertEquals(0, projecao.saldoFinalPagando().signum());
        // 1º mês: juros de 2% sobre 1000 = 20; saldo final = 1000 + 20 - 94,56
        assertEquals(bd("20.00"), projecao.pagandoAParcela().get(0).juros());
        assertEquals(bd("925.44"), projecao.pagandoAParcela().get(0).saldoFinal());
    }

    @Test
    void projecaoSemPagarCresceComJurosCompostos() {
        Divida divida = divida(1, "Loja", "1000.00", "2", "94.56");
        ProjecaoDividaResponse projecao = ProjecaoDividaCalculadora.projetar(divida, 12);

        assertEquals(12, projecao.semPagarNada().size());
        // 1000 * 1,02^12 = 1268,24
        assertEquals(1268.24, projecao.saldoFinalSemPagar().doubleValue(), 0.05);
        assertEquals(268.24, projecao.crescimentoSemPagar().doubleValue(), 0.05);
        assertNull(projecao.encargosAtraso());
    }

    @Test
    void dividaAtrasadaTemMultaDe2PorCentoEMoraProporcional() {
        Divida divida = divida(1, "Banco", "3000.00", "5", "200.00");
        divida.setStatus(StatusDivida.ATRASADA);
        divida.setDiasAtraso(45);
        EncargosAtraso encargos = ProjecaoDividaCalculadora.encargosDoAtraso(divida);

        assertEquals(bd("4.00"), encargos.multa());               // 2% de 200
        assertEquals(bd("3.00"), encargos.jurosMora());           // 200 * 1% * 45/30
        assertEquals(bd("207.00"), encargos.parcelaAtualizada());
        assertEquals(bd("3007.00"), encargos.valorParaQuitarHoje());
    }

    @Test
    void parcelaQueNaoCobreOsJurosGeraAviso() {
        Divida divida = divida(1, "Rotativo", "1000.00", "10", "50.00");
        ProjecaoDividaResponse projecao = ProjecaoDividaCalculadora.projetar(divida, 6);
        assertTrue(projecao.saldoFinalPagando().compareTo(bd("1000.00")) > 0);
        assertEquals(1, projecao.avisos().size());
    }

    // ---------------------------------------------------------------- renegociação

    @Test
    void renegociacaoEscolheOCenarioMaisBarato() {
        Divida divida = divida(1, "Loja", "1000.00", "2", "94.56");
        RenegociacaoResponse resposta = RenegociacaoCalculadora.comparar(divida, bd("40"), 6, bd("1"));

        assertEquals(bd("600.00"), resposta.aVistaComDesconto().totalAPagar());   // 1000 - 40%
        assertEquals(bd("172.55"), resposta.novoParcelamento().valorParcela());   // Price: 1000, 1%, 6x
        assertEquals(bd("1035.30"), resposta.novoParcelamento().totalAPagar());
        assertTrue(resposta.continuarComoEsta().quitavel());
        assertEquals(1134.72, resposta.continuarComoEsta().totalAPagar().doubleValue(), 0.10);
        assertEquals("A_VISTA", resposta.melhorCenario());
        assertEquals(0, resposta.economia().compareTo(
                resposta.continuarComoEsta().totalAPagar().subtract(bd("600.00"))));
    }

    @Test
    void renegociacaoSemJurosDivideEmPartesIguais() {
        assertEquals(bd("100.00"), RenegociacaoCalculadora.novoParcelamento(bd("1200.00"), 12, BigDecimal.ZERO).valorParcela());
        assertEquals(0, RenegociacaoCalculadora.novoParcelamento(bd("1200.00"), 12, BigDecimal.ZERO)
                .totalAPagar().compareTo(bd("1200.00")));
    }

    @Test
    void quandoAParcelaNaoCobreOsJurosContinuarNaoEhQuitavel() {
        Divida divida = divida(1, "Rotativo", "1000.00", "10", "50.00");
        RenegociacaoResponse resposta = RenegociacaoCalculadora.comparar(divida, bd("10"), 12, bd("3"));

        assertFalse(resposta.continuarComoEsta().quitavel());
        assertNull(resposta.economia());
        assertEquals("A_VISTA", resposta.melhorCenario()); // 900,00 contra 12 x 100,46
    }

    @Test
    void pagarAVistaMesmoSemDescontoEvitaOsJuros() {
        Divida divida = divida(1, "Loja", "1000.00", "1", "200.00");
        RenegociacaoResponse resposta = RenegociacaoCalculadora.comparar(divida, bd("0"), 48, bd("8"));
        assertEquals("A_VISTA", resposta.melhorCenario()); // à vista sem desconto = 1000, menor que pagar juros
        assertEquals(bd("1000.00"), resposta.aVistaComDesconto().totalAPagar());
    }

    // ---------------------------------------------------------------- custo em horas

    @Test
    void custoEmHorasDeTrabalho() {
        // renda 4400 / 220 horas = R$ 20 por hora; R$ 150 = 7,5 horas = 7h30
        CustoEmHorasResponse custo = CustoHorasService.calcular(bd("150.00"), bd("4400.00"), 220);
        assertEquals(bd("20.00"), custo.valorHora());
        assertEquals(bd("7.50"), custo.horas());
        assertEquals("7h30", custo.horasFormatadas());
        assertEquals("Essa compra custa 7h30 do seu trabalho", custo.mensagem());
    }

    @Test
    void formatadorDeHorasEDinheiro() {
        assertEquals("0h45", Formatador.horas(0.75));
        assertEquals("12h00", Formatador.horas(12));
        assertEquals("R$ 1.234,50", Formatador.reais(bd("1234.5")));
        assertEquals("13,5%", Formatador.percentual(bd("13.50")));
        assertEquals("2%", Formatador.percentual(bd("2.00")));
    }
}
