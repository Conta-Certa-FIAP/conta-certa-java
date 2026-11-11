package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Random;

import br.com.fiap.contacerta.model.CategoriaPadrao;

/**
 * "Banco de exemplos" de compras realistas por categoria, com faixa de valor em reais.
 * Usado pelo gerador de dados simulados e pelo botão "simular compra".
 */
public final class CatalogoCompras {

    private CatalogoCompras() {
    }

    /** Um exemplo de compra: descrição e faixa de valor (mínimo e máximo em reais). */
    public record ModeloCompra(String descricao, int minimo, int maximo) {
    }

    /** Categorias de saída que fazem sentido para uma compra (lista com ordem fixa). */
    public static final List<String> CATEGORIAS = List.of(
            CategoriaPadrao.MERCADO, CategoriaPadrao.TRANSPORTE, CategoriaPadrao.DELIVERY,
            CategoriaPadrao.COMPRAS, CategoriaPadrao.LAZER, CategoriaPadrao.SAUDE,
            CategoriaPadrao.MORADIA, CategoriaPadrao.EDUCACAO, CategoriaPadrao.ASSINATURAS);

    private static final Map<String, List<ModeloCompra>> COMPRAS = Map.of(
            CategoriaPadrao.MERCADO, List.of(
                    new ModeloCompra("Mercado Extra", 40, 280),
                    new ModeloCompra("Assaí Atacadista", 120, 420),
                    new ModeloCompra("Carrefour", 50, 300),
                    new ModeloCompra("Padaria do bairro", 8, 35),
                    new ModeloCompra("Hortifruti", 20, 90)),
            CategoriaPadrao.TRANSPORTE, List.of(
                    new ModeloCompra("Uber", 9, 45),
                    new ModeloCompra("99", 8, 40),
                    new ModeloCompra("Posto de gasolina", 50, 220),
                    new ModeloCompra("Recarga do Bilhete Único", 20, 100),
                    new ModeloCompra("Estacionamento", 10, 35)),
            CategoriaPadrao.DELIVERY, List.of(
                    new ModeloCompra("iFood", 22, 95),
                    new ModeloCompra("McDonald's", 25, 70),
                    new ModeloCompra("Burger King", 25, 65),
                    new ModeloCompra("Pizzaria", 45, 110),
                    new ModeloCompra("Açaí", 15, 40)),
            CategoriaPadrao.COMPRAS, List.of(
                    new ModeloCompra("Shopee", 20, 180),
                    new ModeloCompra("Mercado Livre", 30, 320),
                    new ModeloCompra("Lojas Renner", 60, 280),
                    new ModeloCompra("Amazon", 30, 260),
                    new ModeloCompra("Salão de beleza", 35, 150)),
            CategoriaPadrao.LAZER, List.of(
                    new ModeloCompra("Cinema", 25, 80),
                    new ModeloCompra("Bar e restaurante", 40, 180),
                    new ModeloCompra("Ingresso de show", 60, 250),
                    new ModeloCompra("Passagem de ônibus", 50, 220)),
            CategoriaPadrao.SAUDE, List.of(
                    new ModeloCompra("Farmácia", 15, 140),
                    new ModeloCompra("Drogaria São Paulo", 20, 160),
                    new ModeloCompra("Consulta médica", 80, 300),
                    new ModeloCompra("Exame laboratorial", 40, 200)),
            CategoriaPadrao.MORADIA, List.of(
                    new ModeloCompra("Gás de cozinha", 100, 130),
                    new ModeloCompra("Conta de celular", 35, 80),
                    new ModeloCompra("Conta de água", 50, 130),
                    new ModeloCompra("Material de limpeza", 20, 90)),
            CategoriaPadrao.EDUCACAO, List.of(
                    new ModeloCompra("Curso online", 30, 200),
                    new ModeloCompra("Papelaria", 15, 90),
                    new ModeloCompra("Livraria", 30, 150)),
            CategoriaPadrao.ASSINATURAS, List.of(
                    new ModeloCompra("Netflix", 21, 60),
                    new ModeloCompra("Spotify", 12, 24),
                    new ModeloCompra("Amazon Prime", 15, 20),
                    new ModeloCompra("Disney+", 28, 62),
                    new ModeloCompra("YouTube Premium", 21, 27)));

    /** A categoria tem exemplos de compra? */
    public static boolean temCategoria(String categoria) {
        return COMPRAS.containsKey(categoria);
    }

    /** Sorteia um exemplo de compra da categoria. */
    public static ModeloCompra sortear(String categoria, Random random) {
        List<ModeloCompra> modelos = COMPRAS.get(categoria);
        return modelos.get(random.nextInt(modelos.size()));
    }

    /** Sorteia um valor dentro da faixa do modelo, com centavos (ex.: 47.90). */
    public static BigDecimal sortearValor(ModeloCompra modelo, Random random) {
        int reais = modelo.minimo() + random.nextInt(modelo.maximo() - modelo.minimo() + 1);
        int centavos = random.nextInt(10) * 10; // ,00 ,10 ,20 ... ,90
        return BigDecimal.valueOf(reais * 100L + centavos, 2);
    }
}
