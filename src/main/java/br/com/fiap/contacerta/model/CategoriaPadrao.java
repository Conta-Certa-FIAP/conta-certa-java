package br.com.fiap.contacerta.model;

import java.util.Set;

/**
 * Nomes das categorias inseridas pelo database/script.sql.
 * A API procura algumas delas pelo nome (ex.: ao registrar um pagamento de dívida),
 * por isso os textos aqui precisam ser IGUAIS aos do script, com acentos.
 */
public final class CategoriaPadrao {

    private CategoriaPadrao() {
    }

    // Entradas
    public static final String SALARIO = "Salário";
    public static final String RENDA_EXTRA = "Renda extra";
    public static final String TRANSFERENCIA_RECEBIDA = "Transferência recebida";
    public static final String RESGATE_COFRE = "Resgate do cofre";

    // Saídas essenciais
    public static final String MERCADO = "Mercado";
    public static final String MORADIA = "Moradia e contas";
    public static final String TRANSPORTE = "Transporte e veículo";
    public static final String SAUDE = "Saúde";
    public static final String EDUCACAO = "Educação";
    public static final String PAGAMENTO_DIVIDAS = "Pagamento de dívidas";

    // Saídas não essenciais
    public static final String DELIVERY = "Fast food e delivery";
    public static final String LAZER = "Lazer e viagem";
    public static final String COMPRAS = "Compras pessoais";
    public static final String ASSINATURAS = "Assinaturas";
    public static final String DEPOSITO_COFRE = "Depósito no cofre";

    /**
     * Categorias "de sistema": as transações delas são criadas pela própria API
     * (pagamento de dívida, depósito e resgate do cofre). Não podem ser lançadas nem
     * excluídas à mão, senão o saldo da dívida/cofre ficaria diferente do extrato.
     */
    private static final Set<String> DE_SISTEMA = Set.of(PAGAMENTO_DIVIDAS, DEPOSITO_COFRE, RESGATE_COFRE);

    public static boolean deSistema(String nomeCategoria) {
        return DE_SISTEMA.contains(nomeCategoria);
    }
}
