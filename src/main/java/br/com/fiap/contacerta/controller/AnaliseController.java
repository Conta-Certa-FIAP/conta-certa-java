package br.com.fiap.contacerta.controller;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.contacerta.dto.analise.ConsultoriaResponse;
import br.com.fiap.contacerta.dto.analise.CustoEmHorasResponse;
import br.com.fiap.contacerta.dto.analise.PagarOuInvestirResponse;
import br.com.fiap.contacerta.dto.analise.ProjecaoDividaResponse;
import br.com.fiap.contacerta.dto.analise.RenegociacaoRequest;
import br.com.fiap.contacerta.dto.analise.RenegociacaoResponse;
import br.com.fiap.contacerta.dto.analise.ResumoMensalResponse;
import br.com.fiap.contacerta.dto.analise.SituacaoResponse;
import br.com.fiap.contacerta.service.AnaliseDividaService;
import br.com.fiap.contacerta.service.ConsultoriaService;
import br.com.fiap.contacerta.service.CustoHorasService;
import br.com.fiap.contacerta.service.PagarOuInvestirService;
import br.com.fiap.contacerta.service.ResumoMensalService;
import br.com.fiap.contacerta.service.SituacaoService;
import jakarta.validation.Valid;

/** Consultoria e análises: rotas só de cálculo (nenhuma delas grava nada no banco). */
@RestController
public class AnaliseController {

    private final SituacaoService situacaoService;
    private final ConsultoriaService consultoriaService;
    private final ResumoMensalService resumoMensalService;
    private final CustoHorasService custoHorasService;
    private final AnaliseDividaService analiseDividaService;
    private final PagarOuInvestirService pagarOuInvestirService;

    public AnaliseController(SituacaoService situacaoService, ConsultoriaService consultoriaService,
                             ResumoMensalService resumoMensalService, CustoHorasService custoHorasService,
                             AnaliseDividaService analiseDividaService,
                             PagarOuInvestirService pagarOuInvestirService) {
        this.situacaoService = situacaoService;
        this.consultoriaService = consultoriaService;
        this.resumoMensalService = resumoMensalService;
        this.custoHorasService = custoHorasService;
        this.analiseDividaService = analiseDividaService;
        this.pagarOuInvestirService = pagarOuInvestirService;
    }

    /** "Pagar dívida ou investir?": compara cada dívida com o rendimento do Tesouro Selic. */
    @GetMapping("/usuarios/{usuarioId}/pagar-ou-investir")
    public PagarOuInvestirResponse pagarOuInvestir(@PathVariable Long usuarioId) {
        return pagarOuInvestirService.comparar(usuarioId);
    }

    /** Situação financeira + nota de saúde (0 a 100) + comprometimento da renda. */
    @GetMapping("/usuarios/{usuarioId}/situacao")
    public SituacaoResponse situacao(@PathVariable Long usuarioId) {
        return situacaoService.avaliar(usuarioId);
    }

    /** Quanto pagar por mês, planos avalanche e bola de neve e sugestões de corte. */
    @GetMapping("/usuarios/{usuarioId}/consultoria")
    public ConsultoriaResponse consultoria(@PathVariable Long usuarioId) {
        return consultoriaService.consultar(usuarioId);
    }

    /** Resumo de um mês (?mes=AAAA-MM; sem o parâmetro, o mês atual). */
    @GetMapping("/usuarios/{usuarioId}/resumo-mensal")
    public ResumoMensalResponse resumoMensal(@PathVariable Long usuarioId,
                                             @RequestParam(required = false) String mes) {
        return resumoMensalService.resumir(usuarioId, mes);
    }

    /** "Essa compra custa 7h30 do seu trabalho" (?valor=150.00). */
    @GetMapping("/usuarios/{usuarioId}/custo-em-horas")
    public CustoEmHorasResponse custoEmHoras(@PathVariable Long usuarioId, @RequestParam BigDecimal valor) {
        return custoHorasService.calcular(usuarioId, valor);
    }

    /** Projeção da dívida mês a mês: pagando a parcela x sem pagar nada (?meses=12). */
    @GetMapping("/dividas/{dividaId}/projecao")
    public ProjecaoDividaResponse projecao(@PathVariable Long dividaId,
                                           @RequestParam(defaultValue = "12") int meses) {
        return analiseDividaService.projetar(dividaId, meses);
    }

    /** Simulador de renegociação. É POST porque recebe um corpo, mas não altera a dívida. */
    @PostMapping("/dividas/{dividaId}/renegociacao/simular")
    public RenegociacaoResponse simularRenegociacao(@PathVariable Long dividaId,
                                                    @Valid @RequestBody RenegociacaoRequest request) {
        return analiseDividaService.simularRenegociacao(dividaId, request);
    }
}
