package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.TotalCategoria;
import br.com.fiap.contacerta.dao.TransacaoDAO;
import br.com.fiap.contacerta.dto.analise.ResumoMensalResponse;
import br.com.fiap.contacerta.dto.analise.ResumoMensalResponse.GastoCategoria;
import br.com.fiap.contacerta.model.TipoMovimento;

/** Resumo de um mês de calendário: entradas, saídas, gastos por categoria e frase de impacto. */
@Service
public class ResumoMensalService {

    private final PerfilFinanceiroService perfilService;
    private final TransacaoDAO transacaoDAO;

    public ResumoMensalService(PerfilFinanceiroService perfilService, TransacaoDAO transacaoDAO) {
        this.perfilService = perfilService;
        this.transacaoDAO = transacaoDAO;
    }

    /** "mes" no formato AAAA-MM; se não vier, usa o mês atual. */
    public ResumoMensalResponse resumir(Long usuarioId, String mes) {
        PerfilFinanceiro perfil = perfilService.montar(usuarioId); // 404 se o usuário não existir
        Periodo periodo = (mes == null || mes.isBlank()) ? Periodo.doMes(YearMonth.now()) : Periodo.doMes(mes);

        List<TotalCategoria> totais =
                transacaoDAO.totaisPorCategoria(usuarioId, periodo.inicio(), periodo.fimExclusivo());

        BigDecimal entradas = somar(totais, TipoMovimento.ENTRADA);
        BigDecimal saidas = somar(totais, TipoMovimento.SAIDA);

        // Base do "% da renda": a renda média; sem ela, as entradas do próprio mês.
        BigDecimal renda = perfil.rendaMedia();
        if (renda == null && entradas.signum() > 0) {
            renda = entradas;
        }
        final BigDecimal rendaConsiderada = renda;

        // O DAO já devolve da maior para a menor.
        List<GastoCategoria> gastos = totais.stream()
                .filter(t -> t.tipo() == TipoMovimento.SAIDA)
                .map(t -> paraGasto(t, rendaConsiderada))
                .toList();

        List<TotalCategoria> naoEssenciais = AnaliseFinanceira.naoEssenciais(totais);
        List<GastoCategoria> top3 = naoEssenciais.stream().limit(3)
                .map(t -> paraGasto(t, rendaConsiderada))
                .toList();

        String frase = null;
        if (!naoEssenciais.isEmpty()) {
            TotalCategoria maior = naoEssenciais.get(0);
            frase = ConsultoriaService.fraseDeImpacto(perfil, maior.categoria(), maior.total());
        }

        return new ResumoMensalResponse(
                YearMonth.from(periodo.inicio()).toString(),
                entradas,
                saidas,
                entradas.subtract(saidas),
                rendaConsiderada,
                gastos,
                top3,
                frase);
    }

    private BigDecimal somar(List<TotalCategoria> totais, TipoMovimento tipo) {
        return totais.stream()
                .filter(t -> t.tipo() == tipo)
                .map(TotalCategoria::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private GastoCategoria paraGasto(TotalCategoria total, BigDecimal renda) {
        return new GastoCategoria(total.categoriaId(), total.categoria(), total.essencial(), total.total(),
                CalculadoraFinanceira.percentual(total.total(), renda));
    }
}
