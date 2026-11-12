package br.com.fiap.contacerta.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.contacerta.dto.investimento.SimulacaoTesouroRequest;
import br.com.fiap.contacerta.dto.investimento.SimulacaoTesouroResponse;
import br.com.fiap.contacerta.service.InvestimentoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/investimentos")
public class InvestimentoController {

    private final InvestimentoService investimentoService;

    public InvestimentoController(InvestimentoService investimentoService) {
        this.investimentoService = investimentoService;
    }

    /** Simulador do Tesouro Selic. É POST porque recebe um corpo, mas não grava nada. */
    @PostMapping("/tesouro-selic/simular")
    public SimulacaoTesouroResponse simularTesouroSelic(@Valid @RequestBody SimulacaoTesouroRequest request) {
        return investimentoService.simularTesouroSelic(request);
    }
}
