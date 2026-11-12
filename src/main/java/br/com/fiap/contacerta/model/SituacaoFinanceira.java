package br.com.fiap.contacerta.model;

/**
 * As quatro situações financeiras possíveis, da melhor para a pior.
 * A ordem das constantes importa: quando mais de uma regra se aplica, vale a mais grave
 * (a que aparece por último aqui).
 */
public enum SituacaoFinanceira {

    CONFORTAVEL("Confortável",
            "Suas parcelas cabem no orçamento e não há dívida atrasada. Continue assim e tente guardar um pouco todo mês."),
    ATENCAO("Atenção",
            "Suas dívidas já pesam no orçamento. Evite novas parcelas e priorize as dívidas com juros mais altos."),
    EM_RISCO("Em risco",
            "Mais da metade da renda está comprometida ou há parcela atrasada. Corte gastos não essenciais e negocie o quanto antes."),
    INADIMPLENTE("Inadimplente",
            "Há dívida atrasada há mais de 90 dias. Procure o credor para renegociar: os juros e a multa aumentam a cada mês.");

    private final String titulo;
    private final String orientacao;

    SituacaoFinanceira(String titulo, String orientacao) {
        this.titulo = titulo;
        this.orientacao = orientacao;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getOrientacao() {
        return orientacao;
    }
}
