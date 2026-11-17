package br.com.fiap.contacerta.service.front;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

import br.com.fiap.contacerta.dto.front.DebtDto;
import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.StatusDivida;
import br.com.fiap.contacerta.model.TipoDivida;

/**
 * Converte uma dívida do banco para o formato do front e calcula o que o front não envia:
 * a média de juros do mercado e o selo (EM_DIA, ATENCAO ou JUROS_ALTOS).
 *
 * Só métodos estáticos e sem banco de dados: dá para testar cada regra sozinha (DividaFrontTest).
 */
public final class DividaFront {

    public static final String EM_DIA = "EM_DIA";
    public static final String ATENCAO = "ATENCAO";
    public static final String JUROS_ALTOS = "JUROS_ALTOS";

    /** Texto que o front mostra (e envia de volta) para cada tipo de dívida. */
    private static final Map<TipoDivida, String> ROTULOS = new EnumMap<>(TipoDivida.class);

    /** Caminho inverso: qual tipo usar quando o front envia um desses textos. */
    private static final Map<String, TipoDivida> TIPO_DO_ROTULO = Map.of(
            "Rotativo do cartão", TipoDivida.CARTAO_ROTATIVO,
            "Fatura parcelada", TipoDivida.CARTAO_PARCELADO,
            "Cheque especial", TipoDivida.CHEQUE_ESPECIAL,
            "Empréstimo pessoal", TipoDivida.EMPRESTIMO_PESSOAL,
            "Empréstimo consignado", TipoDivida.CONSIGNADO,
            "Crediário de loja", TipoDivida.CREDIARIO,
            "Financiamento", TipoDivida.FINANCIAMENTO_VEICULO,
            "Outro", TipoDivida.OUTRO);

    /**
     * Taxa média de juros do mercado para pessoa física, em % ao mês, por tipo de crédito.
     * Valores de referência consultados no SGS do Banco Central (api.bcb.gov.br) em novembro/2026,
     * dado de setembro/2026. Séries: 25477 (rotativo), 25478 (cartão parcelado), 25463 (cheque
     * especial), 25464 (pessoal não consignado), 25469 (consignado), 25471 (veículos),
     * 25497 (imobiliário), 25472 (outros bens) e 25462 (média geral, usada no "outro").
     * São números fixos: para atualizar, basta trocar os valores abaixo.
     */
    private static final Map<TipoDivida, BigDecimal> TAXA_DE_MERCADO = new EnumMap<>(TipoDivida.class);

    static {
        ROTULOS.put(TipoDivida.CARTAO_ROTATIVO, "Rotativo do cartão");
        ROTULOS.put(TipoDivida.CARTAO_PARCELADO, "Fatura parcelada");
        ROTULOS.put(TipoDivida.CHEQUE_ESPECIAL, "Cheque especial");
        ROTULOS.put(TipoDivida.EMPRESTIMO_PESSOAL, "Empréstimo pessoal");
        ROTULOS.put(TipoDivida.CONSIGNADO, "Empréstimo consignado");
        ROTULOS.put(TipoDivida.CREDIARIO, "Crediário de loja");
        ROTULOS.put(TipoDivida.FINANCIAMENTO_VEICULO, "Financiamento");
        ROTULOS.put(TipoDivida.FINANCIAMENTO_IMOVEL, "Financiamento");
        ROTULOS.put(TipoDivida.CONTA_ATRASADA, "Outro");
        ROTULOS.put(TipoDivida.OUTRO, "Outro");

        TAXA_DE_MERCADO.put(TipoDivida.CARTAO_ROTATIVO, new BigDecimal("15.29"));
        TAXA_DE_MERCADO.put(TipoDivida.CARTAO_PARCELADO, new BigDecimal("9.37"));
        TAXA_DE_MERCADO.put(TipoDivida.CHEQUE_ESPECIAL, new BigDecimal("7.53"));
        TAXA_DE_MERCADO.put(TipoDivida.EMPRESTIMO_PESSOAL, new BigDecimal("6.42"));
        TAXA_DE_MERCADO.put(TipoDivida.CONSIGNADO, new BigDecimal("2.11"));
        TAXA_DE_MERCADO.put(TipoDivida.FINANCIAMENTO_VEICULO, new BigDecimal("1.96"));
        TAXA_DE_MERCADO.put(TipoDivida.FINANCIAMENTO_IMOVEL, new BigDecimal("1.11"));
        TAXA_DE_MERCADO.put(TipoDivida.CREDIARIO, new BigDecimal("6.23"));
        TAXA_DE_MERCADO.put(TipoDivida.CONTA_ATRASADA, new BigDecimal("4.12"));
        TAXA_DE_MERCADO.put(TipoDivida.OUTRO, new BigDecimal("4.12"));
    }

    private DividaFront() {
    }

    public static String rotulo(TipoDivida tipo) {
        return ROTULOS.get(tipo);
    }

    /**
     * Tipo da dívida a partir do texto enviado pelo front.
     * O front tem menos opções que o banco ("Financiamento" serve para veículo e imóvel). Por isso,
     * numa edição, se o texto enviado combina com o tipo que a dívida já tinha, mantemos o tipo
     * atual: um financiamento imobiliário não vira financiamento de veículo só por ter sido editado.
     */
    public static TipoDivida tipoDoRotulo(String rotulo, TipoDivida tipoAtual) {
        String texto = rotulo == null ? "" : rotulo.trim();
        if (tipoAtual != null && rotulo(tipoAtual).equals(texto)) {
            return tipoAtual;
        }
        TipoDivida tipo = TIPO_DO_ROTULO.get(texto);
        if (tipo == null) {
            throw new RequisicaoInvalidaException("Tipo de crédito inválido: '" + texto + "'.");
        }
        return tipo;
    }

    public static BigDecimal taxaDeMercado(TipoDivida tipo) {
        return TAXA_DE_MERCADO.get(tipo);
    }

    /** Quantos pontos percentuais a taxa da dívida está acima da média do mercado (negativo = abaixo). */
    public static BigDecimal acimaDoMercado(Divida divida) {
        return divida.getTaxaJurosMensal().subtract(taxaDeMercado(divida.getTipo()));
    }

    /**
     * Selo da dívida:
     *   JUROS_ALTOS - a taxa passa da média do mercado para aquele tipo de crédito;
     *   ATENCAO     - a taxa está dentro da média, mas há parcela atrasada;
     *   EM_DIA      - nenhuma das anteriores.
     */
    public static String status(Divida divida) {
        if (acimaDoMercado(divida).signum() > 0) {
            return JUROS_ALTOS;
        }
        boolean atrasada = divida.getStatus() == StatusDivida.ATRASADA
                || (divida.getDiasAtraso() != null && divida.getDiasAtraso() > 0);
        return atrasada ? ATENCAO : EM_DIA;
    }

    /** O apelido dado pela pessoa; se não houver (dívida simulada), "tipo + credor". */
    public static String nome(Divida divida) {
        if (divida.getNome() != null && !divida.getNome().isBlank()) {
            return divida.getNome();
        }
        return rotulo(divida.getTipo()) + " " + divida.getCredor();
    }

    /** Dívida de 1 parcela só é tratada como "não parcelada" (o front recebe nulo nas parcelas). */
    public static boolean parcelada(Divida divida) {
        return divida.getParcelasTotal() != null && divida.getParcelasTotal() > 1;
    }

    /** Quanto da dívida já foi pago, de 0 a 100: (valor original - saldo devedor) / valor original. */
    public static int progresso(Divida divida) {
        return percentual(pago(divida), divida.getValorOriginal());
    }

    /** Quanto já foi abatido do valor original (nunca negativo, mesmo se os juros fizeram a dívida crescer). */
    public static BigDecimal pago(Divida divida) {
        return divida.getValorOriginal().subtract(divida.getSaldoDevedor()).max(BigDecimal.ZERO);
    }

    /** parte / total em %, arredondado para baixo e limitado a 0..100. Total zero devolve 0. */
    public static int percentual(BigDecimal parte, BigDecimal total) {
        if (total == null || total.signum() <= 0) {
            return 0;
        }
        int valor = parte.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.DOWN).intValue();
        return Math.max(0, Math.min(100, valor));
    }

    public static DebtDto paraDto(Divida divida) {
        boolean parcelada = parcelada(divida);
        return new DebtDto(
                divida.getId(),
                nome(divida),
                rotulo(divida.getTipo()),
                divida.getCredor(),
                divida.getSaldoDevedor(),
                divida.getTaxaJurosMensal(),
                taxaDeMercado(divida.getTipo()),
                parcelada ? divida.getParcelasPagas() : null,
                parcelada ? divida.getParcelasTotal() : null,
                divida.getDataProximoVencimento(),
                divida.getValorParcela(),
                status(divida));
    }
}
