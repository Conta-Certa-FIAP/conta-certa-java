package br.com.fiap.contacerta.service.front;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.fiap.contacerta.dto.front.DebtDto;
import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.StatusDivida;
import br.com.fiap.contacerta.model.TipoDivida;

class DividaFrontTest {

    static Divida divida(TipoDivida tipo, String taxa, int diasAtraso) {
        Divida divida = new Divida();
        divida.setId(1L);
        divida.setCredor("Banco A");
        divida.setTipo(tipo);
        divida.setValorOriginal(new BigDecimal("1000.00"));
        divida.setSaldoDevedor(new BigDecimal("750.00"));
        divida.setTaxaJurosMensal(new BigDecimal(taxa));
        divida.setParcelasTotal(10);
        divida.setParcelasPagas(3);
        divida.setValorParcela(new BigDecimal("120.00"));
        divida.setDataProximoVencimento(LocalDate.of(2026, 12, 10));
        divida.setDiasAtraso(diasAtraso);
        divida.setStatus(diasAtraso > 0 ? StatusDivida.ATRASADA : StatusDivida.EM_DIA);
        return divida;
    }

    @Test
    void taxaAcimaDaMediaDoMercadoGanhaSeloJurosAltos() {
        // média do empréstimo pessoal = 6,42% a.m.
        assertEquals(DividaFront.JUROS_ALTOS, DividaFront.status(divida(TipoDivida.EMPRESTIMO_PESSOAL, "8.000", 0)));
    }

    @Test
    void taxaNaMediaMasAtrasadaGanhaSeloAtencao() {
        assertEquals(DividaFront.ATENCAO, DividaFront.status(divida(TipoDivida.EMPRESTIMO_PESSOAL, "5.000", 12)));
    }

    @Test
    void taxaNaMediaEEmDiaGanhaSeloEmDia() {
        assertEquals(DividaFront.EM_DIA, DividaFront.status(divida(TipoDivida.EMPRESTIMO_PESSOAL, "6.420", 0)));
    }

    @Test
    void semApelidoONomeEhTipoMaisCredor() {
        assertEquals("Rotativo do cartão Banco A", DividaFront.nome(divida(TipoDivida.CARTAO_ROTATIVO, "14.000", 0)));
    }

    @Test
    void progressoEhQuantoJaFoiAbatidoDoValorOriginal() {
        assertEquals(25, DividaFront.progresso(divida(TipoDivida.CONSIGNADO, "1.500", 0)));
    }

    @Test
    void dividaDeUmaParcelaVaiSemParcelasParaOFront() {
        Divida divida = divida(TipoDivida.CHEQUE_ESPECIAL, "7.000", 0);
        divida.setParcelasTotal(1);
        divida.setParcelasPagas(0);
        DebtDto dto = DividaFront.paraDto(divida);
        assertNull(dto.installmentsPaid());
        assertNull(dto.installmentsTotal());
        assertEquals("Cheque especial", dto.type());
        assertEquals(new BigDecimal("7.53"), dto.marketRate());
    }

    @Test
    void edicaoMantemOTipoQuandoORotuloNaoMudou() {
        // "Financiamento" vale para veículo e imóvel: editar não pode trocar um pelo outro.
        assertEquals(TipoDivida.FINANCIAMENTO_IMOVEL,
                DividaFront.tipoDoRotulo("Financiamento", TipoDivida.FINANCIAMENTO_IMOVEL));
        assertEquals(TipoDivida.FINANCIAMENTO_VEICULO, DividaFront.tipoDoRotulo("Financiamento", null));
        assertEquals(TipoDivida.CREDIARIO, DividaFront.tipoDoRotulo("Crediário de loja", TipoDivida.OUTRO));
    }

    @Test
    void rotuloDesconhecidoViraErro400() {
        assertThrows(RequisicaoInvalidaException.class, () -> DividaFront.tipoDoRotulo("Agiota", null));
    }

    @Test
    void percentualFicaEntreZeroECem() {
        assertEquals(0, DividaFront.percentual(BigDecimal.ONE, BigDecimal.ZERO));
        assertEquals(100, DividaFront.percentual(new BigDecimal("300"), new BigDecimal("200")));
        assertEquals(33, DividaFront.percentual(BigDecimal.ONE, new BigDecimal("3")));
    }
}
