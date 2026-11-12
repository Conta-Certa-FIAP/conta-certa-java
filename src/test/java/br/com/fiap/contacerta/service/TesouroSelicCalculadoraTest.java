package br.com.fiap.contacerta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.fiap.contacerta.client.DadoSerie;
import br.com.fiap.contacerta.dto.analise.PagarOuInvestirResponse.ComparacaoDivida;
import br.com.fiap.contacerta.dto.indicador.HistoricoResponse.PontoHistorico;
import br.com.fiap.contacerta.dto.investimento.SimulacaoTesouroResponse;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.TipoDivida;

/** Simulador do Tesouro Selic, tabela do IR, indicadores e "pagar ou investir" (sem banco e sem internet). */
class TesouroSelicCalculadoraTest {

    /** 12,6825% ao ano equivale a exatamente 1% ao mês: deixa as contas fáceis de conferir. */
    private static final BigDecimal SELIC_1_AO_MES = new BigDecimal("12.6825");

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    private static SimulacaoTesouroResponse simular(String inicial, String aporte, int meses, BigDecimal selic, String ipca) {
        return TesouroSelicCalculadora.simular(bd(inicial), bd(aporte), meses, selic, bd(ipca), "teste");
    }

    // ---------------------------------------------------------------- tabela regressiva do IR

    @Test
    void tabelaRegressivaDoImpostoDeRenda() {
        assertEquals(0.225, TesouroSelicCalculadora.aliquotaIr(1));
        assertEquals(0.225, TesouroSelicCalculadora.aliquotaIr(180));
        assertEquals(0.20, TesouroSelicCalculadora.aliquotaIr(181));
        assertEquals(0.20, TesouroSelicCalculadora.aliquotaIr(360));
        assertEquals(0.175, TesouroSelicCalculadora.aliquotaIr(361));
        assertEquals(0.175, TesouroSelicCalculadora.aliquotaIr(720));
        assertEquals(0.15, TesouroSelicCalculadora.aliquotaIr(721));
        assertEquals(0.15, TesouroSelicCalculadora.aliquotaIr(5000));
    }

    // ---------------------------------------------------------------- simulador

    @Test
    void umMesSemAporte() {
        // 1000 a 1% ao mês = 1010 bruto; rendimento 10; IR de 22,5% = 2,25; líquido 1007,75
        SimulacaoTesouroResponse simulacao = simular("1000", "0", 1, SELIC_1_AO_MES, "0");

        assertEquals(bd("1.00"), simulacao.taxaMensalPercentual());
        assertEquals(bd("1000.00"), simulacao.totalInvestido());
        assertEquals(bd("1010.00"), simulacao.valorBrutoFinal());
        assertEquals(bd("10.00"), simulacao.rendimentoBruto());
        assertEquals(bd("2.25"), simulacao.impostoDeRenda());
        assertEquals(bd("1007.75"), simulacao.valorLiquidoFinal());
        assertEquals(bd("7.75"), simulacao.rendimentoLiquido());
        assertEquals(1, simulacao.evolucao().size());
    }

    @Test
    void dozeMesesSemAporteUsaAliquotaDe20PorCento() {
        // 1000 * 1,01^12 = 1126,83; rendimento 126,83; 12 meses = 360 dias -> IR 20% = 25,37
        SimulacaoTesouroResponse simulacao = simular("1000", "0", 12, SELIC_1_AO_MES, "0");
        assertEquals(bd("1126.83"), simulacao.valorBrutoFinal());
        assertEquals(bd("25.37"), simulacao.impostoDeRenda());
        assertEquals(bd("1101.46"), simulacao.valorLiquidoFinal());
        assertEquals(bd("10.15"), simulacao.rentabilidadeLiquidaPercentual());
    }

    @Test
    void cadaAporteTemSeuProprioImposto() {
        // 2 meses, só aportes de 100 no início de cada mês, 1% ao mês:
        //  lote 1 fica 2 meses: 100 * 1,0201 = 102,01 (rende 2,01)
        //  lote 2 fica 1 mês:   100 * 1,01   = 101,00 (rende 1,00)
        //  bruto = 203,01; IR = 22,5% de 3,01 = 0,68; líquido = 202,33
        SimulacaoTesouroResponse simulacao = simular("0", "100", 2, SELIC_1_AO_MES, "0");
        assertEquals(bd("200.00"), simulacao.totalInvestido());
        assertEquals(bd("203.01"), simulacao.valorBrutoFinal());
        assertEquals(bd("0.68"), simulacao.impostoDeRenda());
        assertEquals(bd("202.33"), simulacao.valorLiquidoFinal());
        assertEquals(2, simulacao.evolucao().size());
        assertEquals(bd("100.00"), simulacao.evolucao().get(0).totalInvestido());
    }

    @Test
    void aportesAntigosPagamMenosImpostoQueOsNovos() {
        // Em 36 meses, o IR total fica entre 15% (lotes antigos) e 22,5% (lotes novos) do rendimento.
        SimulacaoTesouroResponse simulacao = simular("0", "500", 36, SELIC_1_AO_MES, "4");
        double aliquotaMedia = simulacao.impostoDeRenda().doubleValue() / simulacao.rendimentoBruto().doubleValue();
        assertTrue(aliquotaMedia > 0.15 && aliquotaMedia < 0.225, "alíquota média " + aliquotaMedia);
        assertEquals(bd("18000.00"), simulacao.totalInvestido());
        assertEquals(36, simulacao.evolucao().size());
    }

    @Test
    void inflacaoReduzOGanhoReal() {
        // rentabilidade líquida de 10,15% em 12 meses com IPCA de 5% ao ano:
        // juro real = 1,1015 / 1,05 - 1 = 4,90%; valor de hoje = 1101,46 / 1,05 = 1049,01
        SimulacaoTesouroResponse simulacao = simular("1000", "0", 12, SELIC_1_AO_MES, "5");
        assertEquals(bd("5.00"), simulacao.ipcaNoPeriodoPercentual());
        assertEquals(4.90, simulacao.juroRealPercentual().doubleValue(), 0.01);
        assertEquals(1049.01, simulacao.valorFinalEmDinheiroDeHoje().doubleValue(), 0.01);
    }

    @Test
    void selicZeroNaoRendeNemPagaImposto() {
        SimulacaoTesouroResponse simulacao = simular("1000", "100", 6, BigDecimal.ZERO, "0");
        assertEquals(bd("1600.00"), simulacao.valorBrutoFinal());
        assertEquals(bd("0.00"), simulacao.impostoDeRenda());
        assertEquals(bd("1600.00"), simulacao.valorLiquidoFinal());
    }

    // ---------------------------------------------------------------- indicadores

    @Test
    void juroReal() {
        // (1,1375 / 1,0458) - 1 = 8,77%
        assertEquals(bd("8.77"), IndicadorService.juroReal(bd("13.75"), bd("4.58")));
    }

    @Test
    void cacheValePor12Horas() {
        LocalDateTime agora = LocalDateTime.of(2026, 11, 11, 15, 0);
        assertTrue(IndicadorService.cacheValido(agora.minusHours(11), agora));
        assertEquals(false, IndicadorService.cacheValido(agora.minusHours(12), agora));
        assertEquals(false, IndicadorService.cacheValido(agora.minusDays(2), agora));
    }

    @Test
    void ficaComOUltimoValorDeCadaMes() {
        List<DadoSerie> dados = List.of(
                new DadoSerie(LocalDate.of(2026, 9, 29), bd("5.40")),
                new DadoSerie(LocalDate.of(2026, 9, 30), bd("5.45")),
                new DadoSerie(LocalDate.of(2026, 10, 1), bd("5.50")),
                new DadoSerie(LocalDate.of(2026, 10, 30), bd("5.45")),
                new DadoSerie(LocalDate.of(2026, 11, 11), bd("5.56")));
        List<DadoSerie> porMes = IndicadorService.ultimoDeCadaMes(dados);

        assertEquals(3, porMes.size());
        assertEquals(LocalDate.of(2026, 9, 30), porMes.get(0).data());
        assertEquals(LocalDate.of(2026, 10, 30), porMes.get(1).data());
        assertEquals(LocalDate.of(2026, 11, 11), porMes.get(2).data());
    }

    @Test
    void historicoComparaComOMesAnterior() {
        List<DadoSerie> porMes = List.of(
                new DadoSerie(LocalDate.of(2026, 8, 31), bd("5.40")),
                new DadoSerie(LocalDate.of(2026, 9, 30), bd("5.45")),
                new DadoSerie(LocalDate.of(2026, 10, 30), bd("5.45")),
                new DadoSerie(LocalDate.of(2026, 11, 11), bd("5.30")));
        List<PontoHistorico> pontos = IndicadorService.montarPontos(porMes, 3);

        assertEquals(3, pontos.size()); // agosto serve só de base para a comparação
        assertEquals("2026-09", pontos.get(0).mes());
        assertEquals("subiu", pontos.get(0).variacao());
        assertEquals("estável", pontos.get(1).variacao());
        assertEquals("caiu", pontos.get(2).variacao());
        assertEquals(bd("-0.15"), pontos.get(2).diferenca());
    }

    // ---------------------------------------------------------------- pagar ou investir

    private static Divida divida(String credor, TipoDivida tipo, String taxa) {
        Divida divida = new Divida();
        divida.setId(1L);
        divida.setCredor(credor);
        divida.setTipo(tipo);
        divida.setTaxaJurosMensal(bd(taxa));
        return divida;
    }

    @Test
    void rendimentoLiquidoDoTesouroDesconta15PorCentoDeIr() {
        // 1% ao mês bruto - 15% de IR = 0,85% ao mês
        assertEquals(bd("0.85"), PagarOuInvestirService.rendimentoMensalLiquido(SELIC_1_AO_MES));
    }

    @Test
    void dividaCaraRecomendaQuitar() {
        ComparacaoDivida comparacao = PagarOuInvestirService.compararDivida(
                divida("Cartão Nubank", TipoDivida.CARTAO_ROTATIVO, "13"), bd("0.85"));
        assertEquals("QUITAR", comparacao.recomendacao());
        assertEquals(bd("15.3"), comparacao.vezesMaisQueInvestir()); // 13 / 0,85
        assertTrue(comparacao.mensagem().contains("rende 15,3 vezes mais que investir"), comparacao.mensagem());
        assertTrue(comparacao.mensagem().contains("13% ao mês"), comparacao.mensagem());
    }

    @Test
    void dividaBarataPodeValerInvestir() {
        ComparacaoDivida comparacao = PagarOuInvestirService.compararDivida(
                divida("Caixa", TipoDivida.FINANCIAMENTO_IMOVEL, "0.80"), bd("0.85"));
        assertEquals("INVESTIR", comparacao.recomendacao());
        assertTrue(comparacao.mensagem().contains("investir pode render mais"), comparacao.mensagem());
    }
}
