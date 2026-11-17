package br.com.fiap.contacerta.service.front;

import java.util.List;
import java.util.Optional;

import br.com.fiap.contacerta.model.TipoDivida;

/**
 * As trilhas de aprendizado (tela "Aprender" do front).
 *
 * O conteúdo é fixo e curto, por isso fica aqui no código (como o CatalogoCompras) e não em
 * tabela. No banco fica só o progresso de cada pessoa (tb_licao_concluida guarda o id da lição).
 * IMPORTANTE: não mude o id de uma lição já publicada, senão o progresso salvo aponta para a lição errada.
 */
public final class CatalogoTrilhas {

    public record Licao(Long id, String titulo, String resumo) {
    }

    public record Trilha(Long id, String titulo, int minutosPorLicao, List<Licao> licoes, String dica) {
    }

    private static final Trilha ROTATIVO = new Trilha(1L, "Como sair do rotativo do cartão", 5, List.of(
            new Licao(11L, "Por que o rotativo é tão caro",
                    "Pagar só o mínimo da fatura joga o resto no crédito mais caro do mercado"),
            new Licao(12L, "Trocar a dívida cara por uma mais barata",
                    "Parcelamento da fatura, empréstimo pessoal e consignado cobram bem menos juros"),
            new Licao(13L, "Como não cair de novo",
                    "Limite do cartão não é renda: regras simples para usar o crédito a seu favor")),
            "Desde 2024, os juros do rotativo não podem passar de 100% do valor original da dívida. "
                    + "Se cobrarem mais que isso, reclame no banco e no Banco Central.");

    private static final Trilha RENEGOCIAR = new Trilha(2L, "Como renegociar uma dívida", 5, List.of(
            new Licao(21L, "Antes de ligar: saiba quanto você deve",
                    "Saldo, juros e parcelas de cada dívida em um só lugar"),
            new Licao(22L, "O que falar com o banco",
                    "Perguntas e direitos do consumidor"),
            new Licao(23L, "Como comparar a proposta",
                    "Total a pagar, valor da parcela e prazo: o que olhar antes de aceitar")),
            "Peça sempre o Custo Efetivo Total (CET) da proposta por escrito antes de aceitar. "
                    + "É ele que mostra quanto a dívida vai custar de verdade.");

    private static final Trilha ORCAMENTO = new Trilha(3L, "Orçamento que cabe no bolso", 5, List.of(
            new Licao(31L, "Para onde vai o seu dinheiro",
                    "Registrar entradas e saídas é o primeiro passo para sobrar no fim do mês"),
            new Licao(32L, "Essencial x não essencial",
                    "Como separar o que não dá para cortar do que dá para reduzir"),
            new Licao(33L, "Reserva de emergência: por onde começar",
                    "Guardar um pouco todo mês evita que um imprevisto vire dívida")),
            "Comece pequeno: guardar um valor fixo logo que o salário cai funciona melhor "
                    + "do que guardar o que sobrar.");

    /** Na ordem em que aparecem na tela. */
    public static final List<Trilha> TRILHAS = List.of(RENEGOCIAR, ROTATIVO, ORCAMENTO);

    private CatalogoTrilhas() {
    }

    public static Optional<Trilha> buscar(Long trilhaId) {
        return TRILHAS.stream().filter(trilha -> trilha.id().equals(trilhaId)).findFirst();
    }

    /** Trilha indicada na tela de detalhe da dívida: rotativo, parcelado e cheque especial têm trilha própria. */
    public static Trilha sugeridaPara(TipoDivida tipo) {
        return tipo.isRotativo() || tipo == TipoDivida.CARTAO_PARCELADO ? ROTATIVO : RENEGOCIAR;
    }
}
