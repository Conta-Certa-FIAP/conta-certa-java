package br.com.fiap.contacerta.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dto.indicador.IndicadoresResponse;
import br.com.fiap.contacerta.dto.investimento.SimulacaoTesouroRequest;
import br.com.fiap.contacerta.dto.investimento.SimulacaoTesouroResponse;
import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;

/** Simulador de investimento no Tesouro Selic. */
@Service
public class InvestimentoService {

    private final IndicadorService indicadorService;

    public InvestimentoService(IndicadorService indicadorService) {
        this.indicadorService = indicadorService;
    }

    public SimulacaoTesouroResponse simularTesouroSelic(SimulacaoTesouroRequest request) {
        if (request.getValorInicial().signum() == 0 && request.getAporteMensal().signum() == 0) {
            throw new RequisicaoInvalidaException("Informe um valor inicial ou um aporte mensal maior que zero.");
        }

        BigDecimal selic = request.getSelicAnual();
        BigDecimal ipca = request.getIpcaAnual();
        String origem = "Taxas informadas na requisição.";

        // Se faltar alguma taxa, usamos os valores atuais do Banco Central (pode dar 503).
        if (selic == null || ipca == null) {
            IndicadoresResponse indicadores = indicadorService.indicadores();
            if (selic == null) {
                selic = indicadores.selic().valor();
            }
            if (ipca == null) {
                ipca = indicadores.ipca12Meses().valor();
            }
            origem = "Banco Central: Selic de " + Formatador.percentual(selic) + " ao ano e IPCA de "
                    + Formatador.percentual(ipca) + " em 12 meses"
                    + (indicadores.desatualizado() ? " (últimos dados salvos: o Banco Central está fora do ar)." : ".");
        }

        return TesouroSelicCalculadora.simular(request.getValorInicial(), request.getAporteMensal(),
                request.getMeses(), selic, ipca, origem);
    }
}
