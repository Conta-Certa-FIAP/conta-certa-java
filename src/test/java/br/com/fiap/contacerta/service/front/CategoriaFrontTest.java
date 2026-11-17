package br.com.fiap.contacerta.service.front;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;
import br.com.fiap.contacerta.model.TipoMovimento;

class CategoriaFrontTest {

    @Test
    void todaCategoriaDoFormularioVoltaComOMesmoCodigo() {
        for (String codigo : new String[] {"MERCADO", "TRANSPORTE", "MORADIA", "CONTAS", "LAZER", "OUTROS"}) {
            assertEquals(codigo, CategoriaFront.codigo(CategoriaFront.nomeNoBanco(TipoMovimento.SAIDA, codigo)));
        }
        for (String codigo : new String[] {"TRABALHO", "OUTROS"}) {
            assertEquals(codigo, CategoriaFront.codigo(CategoriaFront.nomeNoBanco(TipoMovimento.ENTRADA, codigo)));
        }
    }

    @Test
    void categoriaDeSaidaEmEntradaViraErro400() {
        assertThrows(RequisicaoInvalidaException.class,
                () -> CategoriaFront.nomeNoBanco(TipoMovimento.ENTRADA, "MERCADO"));
    }

    @Test
    void categoriaDoBancoSemCodigoProprioViraOutros() {
        assertEquals("OUTROS", CategoriaFront.codigo("Saúde"));
        assertEquals("OUTROS", CategoriaFront.codigo("Depósito no cofre"));
        assertEquals("LAZER", CategoriaFront.codigo("Fast food e delivery"));
    }
}
