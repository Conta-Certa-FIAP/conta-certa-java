package br.com.fiap.contacerta.service;

import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dto.analise.ProjecaoDividaResponse;
import br.com.fiap.contacerta.dto.analise.RenegociacaoRequest;
import br.com.fiap.contacerta.dto.analise.RenegociacaoResponse;
import br.com.fiap.contacerta.exception.RegraNegocioException;
import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.StatusDivida;

/** Análises de UMA dívida: projeção mês a mês e simulação de renegociação. */
@Service
public class AnaliseDividaService {

    private static final int MAXIMO_DE_MESES = 120;

    private final DividaService dividaService;

    public AnaliseDividaService(DividaService dividaService) {
        this.dividaService = dividaService;
    }

    public ProjecaoDividaResponse projetar(Long dividaId, int meses) {
        if (meses < 1 || meses > MAXIMO_DE_MESES) {
            throw new RequisicaoInvalidaException("O parâmetro 'meses' deve estar entre 1 e " + MAXIMO_DE_MESES + ".");
        }
        Divida divida = dividaService.buscarOuFalhar(dividaId);
        return ProjecaoDividaCalculadora.projetar(divida, meses);
    }

    public RenegociacaoResponse simularRenegociacao(Long dividaId, RenegociacaoRequest request) {
        Divida divida = dividaService.buscarOuFalhar(dividaId);
        if (divida.getStatus() == StatusDivida.QUITADA) {
            throw new RegraNegocioException("Esta dívida já está quitada: não há o que renegociar.");
        }
        return RenegociacaoCalculadora.comparar(divida, request.getPercentualDescontoAVista(),
                request.getNovaQtParcelas(), request.getNovaTaxaMensal());
    }
}
