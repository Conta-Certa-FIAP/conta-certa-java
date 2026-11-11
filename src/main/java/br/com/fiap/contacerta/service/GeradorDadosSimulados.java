package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.model.Cartao;
import br.com.fiap.contacerta.model.CategoriaPadrao;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.MeioPagamento;
import br.com.fiap.contacerta.model.StatusDivida;
import br.com.fiap.contacerta.model.TipoCartao;
import br.com.fiap.contacerta.model.TipoDivida;
import br.com.fiap.contacerta.model.TipoMovimento;

/**
 * Open Finance SIMULADO.
 *
 * Num sistema real, ao "conectar um banco" a API buscaria cartões, extrato e dívidas direto
 * do banco do usuário (Open Finance). Aqui nós INVENTAMOS dados realistas para demonstrar o app.
 *
 * Esta classe só monta os objetos em memória (não acessa o banco de dados). Quem grava tudo,
 * dentro de uma transação JDBC, é o ContaService. Por não depender do banco, dá para testar
 * com um Random de semente fixa (ver GeradorDadosSimuladosTest).
 */
@Service
public class GeradorDadosSimulados {

    /** Uma transação ainda sem ids (a categoria vai pelo nome; o service troca pelo id). */
    public record TransacaoSimulada(String categoria, String descricao, BigDecimal valor,
                                    TipoMovimento tipo, MeioPagamento meio, LocalDate data) {
    }

    /** Tudo o que foi gerado para uma conta. */
    public record DadosSimulados(Cartao cartaoDebito, Cartao cartaoCredito,
                                 List<TransacaoSimulada> transacoes, List<Divida> dividas,
                                 BigDecimal saldoFinal) {
    }

    /** Faixa de juros mensais (%) e de quantidade de parcelas de cada tipo de dívida. */
    private record PerfilDivida(double taxaMinima, double taxaMaxima, int parcelasMinimas, int parcelasMaximas) {
    }

    /**
     * Taxas APROXIMADAS, só para a simulação (não são cotações oficiais).
     * Ordem de grandeza do mercado: rotativo e cheque especial são os mais caros;
     * consignado e financiamentos, com garantia, são os mais baratos.
     */
    private static final Map<TipoDivida, PerfilDivida> PERFIS = new EnumMap<>(TipoDivida.class);
    static {
        PERFIS.put(TipoDivida.CARTAO_ROTATIVO, new PerfilDivida(12.0, 15.0, 1, 1));
        PERFIS.put(TipoDivida.CHEQUE_ESPECIAL, new PerfilDivida(7.0, 8.0, 1, 1));
        PERFIS.put(TipoDivida.EMPRESTIMO_PESSOAL, new PerfilDivida(4.0, 6.0, 6, 24));
        PERFIS.put(TipoDivida.CREDIARIO, new PerfilDivida(4.0, 6.0, 4, 12));
        PERFIS.put(TipoDivida.CONSIGNADO, new PerfilDivida(1.7, 2.0, 24, 72));
        PERFIS.put(TipoDivida.FINANCIAMENTO_VEICULO, new PerfilDivida(1.6, 2.5, 36, 60));
        PERFIS.put(TipoDivida.FINANCIAMENTO_IMOVEL, new PerfilDivida(0.8, 1.1, 240, 360));
    }

    private static final int DIA_DO_SALARIO = 5;
    /** As saídas (fora do crédito) ficam em no máximo 85% das entradas: sobra saldo positivo. */
    private static final double TETO_GASTOS = 0.85;
    /** A fatura gerada usa no máximo 60% do limite: sobra limite para o "simular compra". */
    private static final double TETO_FATURA = 0.60;
    /** Compras no crédito só nos últimos dias: representam a fatura atual, ainda em aberto. */
    private static final int DIAS_DA_FATURA = 25;

    /** Lançamento "rascunho": igual à TransacaoSimulada, mas com valor que ainda pode ser ajustado. */
    private static final class Lancamento {
        final String categoria;
        final String descricao;
        BigDecimal valor;
        final TipoMovimento tipo;
        final MeioPagamento meio;
        final LocalDate data;

        Lancamento(String categoria, String descricao, BigDecimal valor,
                   TipoMovimento tipo, MeioPagamento meio, LocalDate data) {
            this.categoria = categoria;
            this.descricao = descricao;
            this.valor = valor;
            this.tipo = tipo;
            this.meio = meio;
            this.data = data;
        }

        boolean saidaQueMexeNoSaldo() {
            return tipo == TipoMovimento.SAIDA && meio != MeioPagamento.CARTAO_CREDITO;
        }

        boolean compraNoCredito() {
            return tipo == TipoMovimento.SAIDA && meio == MeioPagamento.CARTAO_CREDITO;
        }
    }

    /** Versão usada pela API: data de hoje e sorteio de verdade. */
    public DadosSimulados gerar(BigDecimal rendaDeclarada, String nomeBanco) {
        return gerar(rendaDeclarada, nomeBanco, LocalDate.now(), new Random());
    }

    /** Versão com data e Random recebidos de fora (permite teste com resultado repetível). */
    public DadosSimulados gerar(BigDecimal rendaDeclarada, String nomeBanco, LocalDate hoje, Random random) {
        BigDecimal renda = definirRenda(rendaDeclarada, random);
        // Os valores do catálogo foram pensados para uma renda de ~R$ 2.500; ajustamos na proporção.
        double fator = Math.max(0.6, Math.min(2.0, renda.doubleValue() / 2500.0));

        Cartao debito = novoCartao(TipoCartao.DEBITO, random);
        Cartao credito = novoCartao(TipoCartao.CREDITO, random);

        List<LocalDate> salarios = datasDosSalarios(hoje);
        LocalDate inicio = salarios.get(salarios.size() - 1); // salário mais antigo = começo do extrato

        List<Lancamento> lancamentos = new ArrayList<>();
        gerarEntradas(lancamentos, salarios, renda, fator, hoje, random);
        gerarContasFixas(lancamentos, salarios, fator, hoje, random);
        gerarComprasVariaveis(lancamentos, inicio, fator, hoje, random);

        limitarGastosAoOrcamento(lancamentos);
        limitarFaturaAoLimite(lancamentos, credito.getLimite());
        BigDecimal saldoFinal = garantirSaldoNuncaNegativo(lancamentos);

        credito.setFaturaAtual(somar(lancamentos, Lancamento::compraNoCredito));

        List<TransacaoSimulada> transacoes = lancamentos.stream()
                .map(l -> new TransacaoSimulada(l.categoria, l.descricao, l.valor, l.tipo, l.meio, l.data))
                .toList();
        List<Divida> dividas = gerarDividas(renda, nomeBanco, hoje, random);

        return new DadosSimulados(debito, credito, transacoes, dividas, saldoFinal);
    }

    // ------------------------------------------------------------------ renda e cartões

    /** Usa a renda declarada no cadastro; se não houver, sorteia entre R$ 1.500 e R$ 4.500. */
    private BigDecimal definirRenda(BigDecimal rendaDeclarada, Random random) {
        if (rendaDeclarada != null && rendaDeclarada.signum() > 0) {
            return rendaDeclarada.setScale(2, RoundingMode.HALF_UP);
        }
        int sorteada = 1500 + random.nextInt(301) * 10; // múltiplos de 10 entre 1500 e 4500
        return BigDecimal.valueOf(sorteada).setScale(2);
    }

    private Cartao novoCartao(TipoCartao tipo, Random random) {
        Cartao cartao = new Cartao();
        cartao.setTipo(tipo);
        cartao.setNumeroFinal(String.format("%04d", random.nextInt(10000)));
        cartao.setFaturaAtual(BigDecimal.ZERO.setScale(2));
        cartao.setDataCadastro(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        if (tipo == TipoCartao.CREDITO) {
            int limite = 800 + random.nextInt(43) * 100; // R$ 800 a R$ 5.000, de 100 em 100
            cartao.setLimite(BigDecimal.valueOf(limite).setScale(2));
            cartao.setDiaVencimento(1 + random.nextInt(28)); // dia 1 a 28
        }
        return cartao;
    }

    // ------------------------------------------------------------------ extrato

    /**
     * Os 3 últimos dias de pagamento (dia 5) até hoje, do mais recente para o mais antigo.
     * Qualquer janela de 3 meses contém exatamente 3 "dias 5": por isso geramos 1 salário por mês.
     */
    static List<LocalDate> datasDosSalarios(LocalDate hoje) {
        LocalDate ultimo = hoje.getDayOfMonth() >= DIA_DO_SALARIO
                ? hoje.withDayOfMonth(DIA_DO_SALARIO)
                : hoje.minusMonths(1).withDayOfMonth(DIA_DO_SALARIO);
        return List.of(ultimo, ultimo.minusMonths(1), ultimo.minusMonths(2));
    }

    private void gerarEntradas(List<Lancamento> lancamentos, List<LocalDate> salarios, BigDecimal renda,
                               double fator, LocalDate hoje, Random random) {
        String[] extras = {"Bico de fim de semana", "Venda de usados", "Serviço extra (freela)"};
        for (LocalDate diaDoSalario : salarios) {
            lancamentos.add(new Lancamento(CategoriaPadrao.SALARIO, "Salário", renda,
                    TipoMovimento.ENTRADA, MeioPagamento.TRANSFERENCIA, diaDoSalario));

            // Em cerca de 1 a cada 3 meses aparece uma renda extra
            if (random.nextInt(100) < 35) {
                LocalDate data = dataEntre(diaDoSalario, menor(diaDoSalario.plusDays(27), hoje), random);
                BigDecimal valor = dinheiro((80 + random.nextInt(521)) * fator);
                lancamentos.add(new Lancamento(CategoriaPadrao.RENDA_EXTRA, extras[random.nextInt(extras.length)],
                        valor, TipoMovimento.ENTRADA, MeioPagamento.PIX, data));
            }
        }
    }

    /** Contas que se repetem todo mês: aluguel, luz, internet e uma assinatura. */
    private void gerarContasFixas(List<Lancamento> lancamentos, List<LocalDate> salarios,
                                  double fator, LocalDate hoje, Random random) {
        // Aluguel, internet e assinatura têm o MESMO valor todos os meses; a luz varia.
        BigDecimal aluguel = dinheiro((600 + random.nextInt(601)) * fator);
        BigDecimal internet = dinheiro(80 + random.nextInt(51));
        CatalogoCompras.ModeloCompra assinatura = CatalogoCompras.sortear(CategoriaPadrao.ASSINATURAS, random);
        BigDecimal valorAssinatura = CatalogoCompras.sortearValor(assinatura, random);

        for (LocalDate diaDoSalario : salarios) {
            adicionarSeNaoForFuturo(lancamentos, hoje, new Lancamento(CategoriaPadrao.MORADIA, "Aluguel", aluguel,
                    TipoMovimento.SAIDA, MeioPagamento.BOLETO, diaDoSalario.plusDays(1 + random.nextInt(5))));
            adicionarSeNaoForFuturo(lancamentos, hoje, new Lancamento(CategoriaPadrao.MORADIA, "Conta de luz",
                    dinheiro((90 + random.nextInt(151)) * fator),
                    TipoMovimento.SAIDA, MeioPagamento.BOLETO, diaDoSalario.plusDays(5 + random.nextInt(11))));
            adicionarSeNaoForFuturo(lancamentos, hoje, new Lancamento(CategoriaPadrao.MORADIA, "Internet fibra", internet,
                    TipoMovimento.SAIDA, MeioPagamento.PIX, diaDoSalario.plusDays(3 + random.nextInt(10))));
            adicionarSeNaoForFuturo(lancamentos, hoje, new Lancamento(CategoriaPadrao.ASSINATURAS, assinatura.descricao(),
                    valorAssinatura, TipoMovimento.SAIDA, MeioPagamento.CARTAO_DEBITO,
                    diaDoSalario.plusDays(random.nextInt(21))));
        }
    }

    private void adicionarSeNaoForFuturo(List<Lancamento> lancamentos, LocalDate hoje, Lancamento lancamento) {
        if (!lancamento.data.isAfter(hoje)) {
            lancamentos.add(lancamento);
        }
    }

    /** Completa o extrato até um total de 30 a 45 saídas, espalhadas pelas categorias. */
    private void gerarComprasVariaveis(List<Lancamento> lancamentos, LocalDate inicio,
                                       double fator, LocalDate hoje, Random random) {
        int totalDeSaidas = 30 + random.nextInt(16); // 30 a 45
        long jaCriadas = lancamentos.stream().filter(l -> l.tipo == TipoMovimento.SAIDA).count();

        for (long i = jaCriadas; i < totalDeSaidas; i++) {
            String categoria = sortearCategoria(random);
            CatalogoCompras.ModeloCompra modelo = CatalogoCompras.sortear(categoria, random);
            BigDecimal valor = dinheiro(CatalogoCompras.sortearValor(modelo, random).doubleValue() * fator);
            LocalDate data = dataEntre(inicio, hoje, random);
            lancamentos.add(new Lancamento(categoria, modelo.descricao(), valor,
                    TipoMovimento.SAIDA, sortearMeio(data, hoje, random), data));
        }
    }

    /** Sorteio "com peso": mercado, transporte e delivery aparecem mais que educação, por exemplo. */
    private String sortearCategoria(Random random) {
        // Mesma ordem de CatalogoCompras.CATEGORIAS; os pesos somam 100.
        int[] pesos = {24, 16, 16, 10, 9, 8, 5, 6, 6};
        int sorteio = random.nextInt(100);
        int acumulado = 0;
        for (int i = 0; i < pesos.length; i++) {
            acumulado += pesos[i];
            if (sorteio < acumulado) {
                return CatalogoCompras.CATEGORIAS.get(i);
            }
        }
        return CatalogoCompras.CATEGORIAS.get(0);
    }

    private MeioPagamento sortearMeio(LocalDate data, LocalDate hoje, Random random) {
        boolean recente = !data.isBefore(hoje.minusDays(DIAS_DA_FATURA));
        if (recente && random.nextInt(100) < 35) {
            return MeioPagamento.CARTAO_CREDITO;
        }
        return random.nextBoolean() ? MeioPagamento.PIX : MeioPagamento.CARTAO_DEBITO;
    }

    // ------------------------------------------------------------------ coerência do extrato

    /** Se as saídas passarem do teto (85% das entradas), reduz todas na mesma proporção. */
    private void limitarGastosAoOrcamento(List<Lancamento> lancamentos) {
        BigDecimal entradas = somar(lancamentos, l -> l.tipo == TipoMovimento.ENTRADA);
        BigDecimal saidas = somar(lancamentos, Lancamento::saidaQueMexeNoSaldo);
        BigDecimal teto = entradas.multiply(BigDecimal.valueOf(TETO_GASTOS));
        if (saidas.compareTo(teto) > 0) {
            double proporcao = teto.doubleValue() / saidas.doubleValue();
            lancamentos.stream().filter(Lancamento::saidaQueMexeNoSaldo)
                    .forEach(l -> l.valor = dinheiro(Math.max(1.0, l.valor.doubleValue() * proporcao)));
        }
    }

    /** Se as compras no crédito passarem de 60% do limite, reduz todas na mesma proporção. */
    private void limitarFaturaAoLimite(List<Lancamento> lancamentos, BigDecimal limite) {
        BigDecimal fatura = somar(lancamentos, Lancamento::compraNoCredito);
        BigDecimal teto = limite.multiply(BigDecimal.valueOf(TETO_FATURA));
        if (fatura.compareTo(teto) > 0) {
            double proporcao = teto.doubleValue() / fatura.doubleValue();
            lancamentos.stream().filter(Lancamento::compraNoCredito)
                    .forEach(l -> l.valor = dinheiro(Math.max(1.0, l.valor.doubleValue() * proporcao)));
        }
    }

    /**
     * Ordena por data e "passa o extrato" conferindo o saldo dia a dia.
     * Se uma saída deixaria o saldo negativo, ela é reduzida para caber (40% do saldo do momento).
     * Devolve o saldo final, que portanto é sempre coerente com as transações geradas.
     */
    private BigDecimal garantirSaldoNuncaNegativo(List<Lancamento> lancamentos) {
        // Mesma data: a entrada vem antes da saída (o salário cai antes das compras do dia).
        lancamentos.sort(Comparator.comparing((Lancamento l) -> l.data)
                .thenComparing(l -> l.tipo == TipoMovimento.ENTRADA ? 0 : 1));

        BigDecimal saldo = BigDecimal.ZERO.setScale(2);
        BigDecimal umReal = BigDecimal.ONE.setScale(2);

        Iterator<Lancamento> iterador = lancamentos.iterator();
        while (iterador.hasNext()) {
            Lancamento lancamento = iterador.next();
            if (lancamento.tipo == TipoMovimento.ENTRADA) {
                saldo = saldo.add(lancamento.valor);
            } else if (lancamento.saidaQueMexeNoSaldo()) {
                if (lancamento.valor.compareTo(saldo) > 0) {
                    if (saldo.compareTo(umReal) < 0) {
                        iterador.remove(); // sem saldo nem para R$ 1: descarta (caso raríssimo)
                        continue;
                    }
                    lancamento.valor = dinheiro(Math.max(1.0, saldo.doubleValue() * 0.4));
                }
                saldo = saldo.subtract(lancamento.valor);
            }
            // compra no crédito não mexe no saldo
        }
        return saldo;
    }

    // ------------------------------------------------------------------ dívidas

    /** De 0 a 3 dívidas de tipos diferentes. */
    private List<Divida> gerarDividas(BigDecimal renda, String nomeBanco, LocalDate hoje, Random random) {
        List<TipoDivida> tipos = new ArrayList<>(PERFIS.keySet());
        Collections.shuffle(tipos, random);
        int quantidade = random.nextInt(4);

        List<Divida> dividas = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            dividas.add(gerarDivida(tipos.get(i), renda, nomeBanco, hoje, random));
        }
        return dividas;
    }

    private Divida gerarDivida(TipoDivida tipo, BigDecimal renda, String nomeBanco, LocalDate hoje, Random random) {
        PerfilDivida perfil = PERFIS.get(tipo);
        double taxaSorteada = perfil.taxaMinima() + random.nextDouble() * (perfil.taxaMaxima() - perfil.taxaMinima());
        BigDecimal taxa = dinheiro(taxaSorteada);

        Divida divida = new Divida();
        divida.setTipo(tipo);
        divida.setCredor(sortearCredor(tipo, nomeBanco, random));
        divida.setTaxaJurosMensal(taxa);
        divida.setSimulada(true);

        if (tipo.isRotativo()) {
            // Rotativo/cheque especial não têm parcela fixa: a dívida fica entre 20% e 80% da renda
            // e a "parcela" é um pagamento mínimo simulado = juros do mês + 5% do saldo.
            BigDecimal saldo = dinheiro(renda.doubleValue() * (0.20 + random.nextDouble() * 0.60));
            divida.setValorOriginal(saldo);
            divida.setSaldoDevedor(saldo);
            divida.setParcelasTotal(1);
            divida.setParcelasPagas(0);
            divida.setValorParcela(dinheiro(saldo.doubleValue() * (CalculadoraFinanceira.fracao(taxa) + 0.05)));
            divida.setDataContratacao(hoje.minusDays(20 + random.nextInt(61)));
        } else {
            // Parcelados: escolhemos uma parcela que caiba na renda (5% a 18%) e descobrimos o
            // valor financiado pela Price. Assim a dívida simulada combina com a renda da pessoa.
            int parcelas = perfil.parcelasMinimas()
                    + random.nextInt(perfil.parcelasMaximas() - perfil.parcelasMinimas() + 1);
            BigDecimal parcelaAlvo = dinheiro(renda.doubleValue() * (0.05 + random.nextDouble() * 0.13));
            BigDecimal bruto = CalculadoraFinanceira.valorPresente(parcelaAlvo, taxa, parcelas);
            // arredonda o valor financiado para baixo, em dezenas (ex.: 4.237,55 -> 4.230,00)
            BigDecimal valor = bruto.divide(BigDecimal.TEN, 0, RoundingMode.DOWN).multiply(BigDecimal.TEN)
                    .max(BigDecimal.valueOf(100)).setScale(2);

            int maximoPagas = Math.min(parcelas - 1, (int) (parcelas * 0.4));
            int pagas = random.nextInt(maximoPagas + 1);

            divida.setValorOriginal(valor);
            divida.setParcelasTotal(parcelas);
            divida.setParcelasPagas(pagas);
            divida.setValorParcela(CalculadoraFinanceira.parcelaPrice(valor, taxa, parcelas));
            divida.setSaldoDevedor(CalculadoraFinanceira.saldoAposParcelas(valor, taxa, parcelas, pagas));
            divida.setDataContratacao(hoje.minusMonths(pagas + 1L).minusDays(random.nextInt(20)));
        }

        // Cerca de 1 em cada 4 dívidas nasce atrasada (a maioria com até 90 dias).
        if (random.nextInt(100) < 25) {
            int dias = random.nextInt(100) < 75 ? 5 + random.nextInt(86) : 91 + random.nextInt(60);
            divida.setStatus(StatusDivida.ATRASADA);
            divida.setDiasAtraso(dias);
            divida.setDataProximoVencimento(hoje.minusDays(dias)); // venceu há "dias" dias
        } else {
            divida.setStatus(StatusDivida.EM_DIA);
            divida.setDiasAtraso(0);
            divida.setDataProximoVencimento(hoje.plusDays(1 + random.nextInt(28)));
        }
        return divida;
    }

    private String sortearCredor(TipoDivida tipo, String nomeBanco, Random random) {
        String[] opcoes = switch (tipo) {
            case CARTAO_ROTATIVO -> new String[] {"Cartão " + nomeBanco};
            case CHEQUE_ESPECIAL -> new String[] {nomeBanco};
            case EMPRESTIMO_PESSOAL -> new String[] {nomeBanco, "Crefisa", "Banco Pan", "Mercado Pago"};
            case CREDIARIO -> new String[] {"Casas Bahia", "Magazine Luiza", "Pernambucanas", "Lojas Marisa"};
            case CONSIGNADO -> new String[] {nomeBanco, "Banco BMG", "Banco Pan"};
            case FINANCIAMENTO_VEICULO -> new String[] {"BV Financeira", "Santander Financiamentos"};
            case FINANCIAMENTO_IMOVEL -> new String[] {"Caixa Econômica Federal"};
            default -> new String[] {nomeBanco};
        };
        return opcoes[random.nextInt(opcoes.length)];
    }

    // ------------------------------------------------------------------ apoio

    private static BigDecimal dinheiro(double valor) {
        return CalculadoraFinanceira.arredondar(valor);
    }

    private static LocalDate menor(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    /** Data aleatória entre "inicio" e "fim" (as duas inclusive). */
    private static LocalDate dataEntre(LocalDate inicio, LocalDate fim, Random random) {
        int dias = (int) ChronoUnit.DAYS.between(inicio, fim);
        return inicio.plusDays(random.nextInt(Math.max(dias, 0) + 1));
    }

    private static BigDecimal somar(List<Lancamento> lancamentos, java.util.function.Predicate<Lancamento> filtro) {
        return lancamentos.stream().filter(filtro)
                .map(l -> l.valor)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
}
