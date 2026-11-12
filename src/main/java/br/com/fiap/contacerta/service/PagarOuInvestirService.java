package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.DividaDAO;
import br.com.fiap.contacerta.dto.analise.PagarOuInvestirResponse;
import br.com.fiap.contacerta.dto.analise.PagarOuInvestirResponse.ComparacaoDivida;
import br.com.fiap.contacerta.dto.indicador.IndicadoresResponse;
import br.com.fiap.contacerta.model.Divida;

/**
 * "Pagar dívida ou investir?"
 * Compara a taxa mensal de cada dívida com o que o Tesouro Selic rende por mês, já descontado
 * o Imposto de Renda. Se a dívida cobra mais do que o investimento rende, quitar "rende" mais.
 */
@Service
public class PagarOuInvestirService {

    /** IR de 15%: alíquota de quem deixa o dinheiro investido por mais de 2 anos (referência de longo prazo). */
    static final BigDecimal ALIQUOTA_IR = BigDecimal.valueOf(15);

    private static final String COMO_CALCULAMOS = "Rendimento mensal do Tesouro Selic = (1 + Selic anual)^(1/12) - 1. "
            + "Descontamos 15% de Imposto de Renda (alíquota para mais de 2 anos) para chegar ao rendimento líquido. "
            + "Depois dividimos a taxa mensal de cada dívida por esse rendimento.";

    private final UsuarioService usuarioService;
    private final DividaDAO dividaDAO;
    private final IndicadorService indicadorService;

    public PagarOuInvestirService(UsuarioService usuarioService, DividaDAO dividaDAO,
                                  IndicadorService indicadorService) {
        this.usuarioService = usuarioService;
        this.dividaDAO = dividaDAO;
        this.indicadorService = indicadorService;
    }

    public PagarOuInvestirResponse comparar(Long usuarioId) {
        usuarioService.buscarOuFalhar(usuarioId);
        List<Divida> ativas = dividaDAO.listarPorUsuario(usuarioId).stream().filter(Divida::ativa).toList();

        IndicadoresResponse indicadores = indicadorService.indicadores(); // pode dar 503
        BigDecimal selic = indicadores.selic().valor();
        BigDecimal rendimentoLiquido = rendimentoMensalLiquido(selic);

        List<ComparacaoDivida> comparacoes = ativas.stream()
                .map(divida -> compararDivida(divida, rendimentoLiquido))
                .toList();

        List<String> avisos = new ArrayList<>();
        if (ativas.isEmpty()) {
            avisos.add("Você não tem dívidas ativas: todo valor que sobrar pode ser investido. O Tesouro Selic rende cerca de "
                    + Formatador.percentual(rendimentoLiquido) + " ao mês líquido.");
        }
        return new PagarOuInvestirResponse(selic, rendimentoLiquido, ALIQUOTA_IR, comparacoes, COMO_CALCULAMOS,
                indicadores.desatualizado(), avisos);
    }

    // ------------------------------------------------------------------ cálculos (estáticos e testáveis)

    /** Quanto o Tesouro Selic rende por mês, em %, já sem os 15% de IR. */
    static BigDecimal rendimentoMensalLiquido(BigDecimal selicAnual) {
        double bruto = CalculadoraFinanceira.taxaMensalEquivalente(selicAnual.doubleValue());
        double liquido = bruto * (1 - ALIQUOTA_IR.doubleValue() / 100.0);
        return CalculadoraFinanceira.arredondar(liquido * 100);
    }

    static ComparacaoDivida compararDivida(Divida divida, BigDecimal rendimentoLiquido) {
        BigDecimal taxa = divida.getTaxaJurosMensal();
        boolean quitarCompensa = taxa.compareTo(rendimentoLiquido) > 0;
        BigDecimal vezes = rendimentoLiquido.signum() > 0
                ? taxa.divide(rendimentoLiquido, 1, RoundingMode.HALF_UP)
                : null;

        String inicio = "Sua dívida com " + divida.getCredor() + " (" + divida.getTipo().getDescricao() + ") cobra "
                + Formatador.percentual(taxa) + " ao mês";
        String mensagem;
        if (!quitarCompensa) {
            mensagem = inicio + ", menos do que o Tesouro Selic rende líquido (" + Formatador.percentual(rendimentoLiquido)
                    + " ao mês): neste caso investir pode render mais do que antecipar o pagamento.";
        } else if (vezes != null && vezes.compareTo(new BigDecimal("1.5")) >= 0) {
            mensagem = inicio + " e o Tesouro Selic rende cerca de " + Formatador.percentual(rendimentoLiquido)
                    + " ao mês líquido: quitar essa dívida rende " + Formatador.numero(vezes)
                    + " vezes mais que investir.";
        } else {
            mensagem = inicio + " e o Tesouro Selic rende cerca de " + Formatador.percentual(rendimentoLiquido)
                    + " ao mês líquido: quitar essa dívida rende mais que investir.";
        }

        return new ComparacaoDivida(divida.getId(), divida.getCredor(), divida.getTipo().getDescricao(), taxa,
                vezes, quitarCompensa ? "QUITAR" : "INVESTIR", mensagem);
    }
}
