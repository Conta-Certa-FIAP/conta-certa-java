package br.com.fiap.contacerta.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.contacerta.dto.indicador.HistoricoResponse;
import br.com.fiap.contacerta.dto.indicador.IndicadoresResponse;
import br.com.fiap.contacerta.service.IndicadorService;

/** "Situação do país": indicadores econômicos reais do Banco Central. */
@RestController
@RequestMapping("/indicadores")
public class IndicadorController {

    private final IndicadorService indicadorService;

    public IndicadorController(IndicadorService indicadorService) {
        this.indicadorService = indicadorService;
    }

    /** Selic, IPCA mensal, IPCA 12 meses, dólar e juro real. 503 se o Banco Central cair e não houver cache. */
    @GetMapping
    public IndicadoresResponse indicadores() {
        return indicadorService.indicadores();
    }

    /** Histórico mensal para os gráficos (?meses=12, de 1 a 24). */
    @GetMapping("/historico")
    public HistoricoResponse historico(@RequestParam(defaultValue = "12") int meses) {
        return indicadorService.historico(meses);
    }
}
