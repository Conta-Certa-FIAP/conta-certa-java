package br.com.fiap.contacerta.service;

/**
 * Regras do CPF: limpar a máscara, validar os dígitos verificadores e mascarar para exibição.
 * Métodos estáticos e sem banco de dados: fáceis de testar (ver CpfUtilTest).
 */
public final class CpfUtil {

    private CpfUtil() {
    }

    /** "529.982.247-25" -> "52998224725". É assim (só números) que o CPF é guardado no banco. */
    public static String apenasNumeros(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("\\D", "");
    }

    /**
     * Valida o CPF. Aceita "52998224725" ou "529.982.247-25".
     *
     * Como funciona o dígito verificador:
     *  - 1º dígito: multiplica os 9 primeiros números por 10, 9, 8... 2, soma tudo,
     *    calcula (soma * 10) % 11 (se der 10, vira 0). Tem que ser igual ao 10º número.
     *  - 2º dígito: mesma conta com os 10 primeiros números e pesos 11, 10, 9... 2.
     */
    public static boolean valido(String cpf) {
        if (cpf == null) {
            return false;
        }
        String texto = cpf.trim();
        // Só dois formatos são aceitos: 11 números seguidos ou a máscara completa.
        if (!texto.matches("\\d{11}") && !texto.matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}")) {
            return false;
        }
        String numeros = apenasNumeros(texto);

        // 111.111.111-11, 222.222.222-22... passam na conta dos dígitos, mas não existem.
        if (numeros.chars().distinct().count() == 1) {
            return false;
        }

        int primeiro = calcularDigito(numeros, 9);
        int segundo = calcularDigito(numeros, 10);
        return primeiro == numeros.charAt(9) - '0' && segundo == numeros.charAt(10) - '0';
    }

    /** Calcula um dígito verificador usando os primeiros "quantidade" números do CPF. */
    private static int calcularDigito(String numeros, int quantidade) {
        int soma = 0;
        int peso = quantidade + 1;
        for (int i = 0; i < quantidade; i++) {
            soma += (numeros.charAt(i) - '0') * peso;
            peso--;
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    /**
     * "52998224725" -> "***.982.247-**".
     * A API nunca devolve o CPF inteiro: mostramos só o meio, o suficiente para a pessoa
     * reconhecer o próprio documento (cuidado com dados pessoais / LGPD).
     */
    public static String mascarar(String cpf) {
        String numeros = apenasNumeros(cpf);
        if (numeros.length() != 11) {
            return "***.***.***-**";
        }
        return "***." + numeros.substring(3, 6) + "." + numeros.substring(6, 9) + "-**";
    }
}
