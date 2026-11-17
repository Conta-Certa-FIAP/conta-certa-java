package br.com.fiap.contacerta.service.front;

import java.util.Map;

import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;
import br.com.fiap.contacerta.model.CategoriaPadrao;
import br.com.fiap.contacerta.model.TipoMovimento;

/**
 * "Tradutor" de categorias entre o front e o banco.
 *
 * O front trabalha com 7 códigos fixos (MERCADO, TRANSPORTE, MORADIA, CONTAS, LAZER, TRABALHO
 * e OUTROS). O banco tem 16 categorias com nome (tb_categoria). Aqui ficam as duas direções:
 *  - ao GRAVAR: código do front -> nome da categoria no banco;
 *  - ao LER:    nome da categoria no banco -> código do front.
 * A ida e a volta combinam: uma transação lançada como CONTAS volta como CONTAS.
 */
public final class CategoriaFront {

    public static final String OUTROS = "OUTROS";

    /** Código do front -> categoria do banco, para SAÍDAS. */
    private static final Map<String, String> SAIDAS = Map.of(
            "MERCADO", CategoriaPadrao.MERCADO,
            "TRANSPORTE", CategoriaPadrao.TRANSPORTE,
            "MORADIA", CategoriaPadrao.MORADIA,
            "CONTAS", CategoriaPadrao.CONTAS_CONSUMO,
            "LAZER", CategoriaPadrao.LAZER,
            OUTROS, CategoriaPadrao.COMPRAS);

    /** Código do front -> categoria do banco, para ENTRADAS. */
    private static final Map<String, String> ENTRADAS = Map.of(
            "TRABALHO", CategoriaPadrao.SALARIO,
            OUTROS, CategoriaPadrao.RENDA_EXTRA);

    /** Categoria do banco -> código do front. O que não está aqui vira OUTROS. */
    private static final Map<String, String> CODIGOS = Map.ofEntries(
            Map.entry(CategoriaPadrao.SALARIO, "TRABALHO"),
            Map.entry(CategoriaPadrao.MERCADO, "MERCADO"),
            Map.entry(CategoriaPadrao.TRANSPORTE, "TRANSPORTE"),
            Map.entry(CategoriaPadrao.MORADIA, "MORADIA"),
            Map.entry(CategoriaPadrao.CONTAS_CONSUMO, "CONTAS"),
            Map.entry(CategoriaPadrao.ASSINATURAS, "CONTAS"),
            Map.entry(CategoriaPadrao.PAGAMENTO_DIVIDAS, "CONTAS"),
            Map.entry(CategoriaPadrao.LAZER, "LAZER"),
            Map.entry(CategoriaPadrao.DELIVERY, "LAZER"));

    private CategoriaFront() {
    }

    /** Nome da categoria do banco para um lançamento do front. Combinação inválida vira 400. */
    public static String nomeNoBanco(TipoMovimento tipo, String codigoDoFront) {
        Map<String, String> mapa = tipo == TipoMovimento.ENTRADA ? ENTRADAS : SAIDAS;
        String nome = mapa.get(codigoDoFront);
        if (nome == null) {
            throw new RequisicaoInvalidaException(tipo == TipoMovimento.ENTRADA
                    ? "Para entradas, a categoria deve ser TRABALHO ou OUTROS."
                    : "Para saídas, a categoria deve ser MERCADO, TRANSPORTE, MORADIA, CONTAS, LAZER ou OUTROS.");
        }
        return nome;
    }

    /** Código do front para uma categoria do banco. */
    public static String codigo(String nomeNoBanco) {
        return CODIGOS.getOrDefault(nomeNoBanco, OUTROS);
    }
}
