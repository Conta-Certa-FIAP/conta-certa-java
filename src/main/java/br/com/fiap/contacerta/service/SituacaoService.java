package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dto.analise.SituacaoResponse;
import br.com.fiap.contacerta.model.SituacaoFinanceira;
import br.com.fiap.contacerta.service.AnaliseFinanceira.NotaSaude;

/** Monta a resposta de GET /usuarios/{id}/situacao (situação + nota + comprometimento). */
@Service
public class SituacaoService {

    static final String AVISO_SEM_RENDA = "Não encontramos entradas nos últimos 3 meses nem renda declarada. "
            + "Informe sua renda mensal no cadastro para calcularmos o comprometimento e a nota completa.";

    private final PerfilFinanceiroService perfilService;

    public SituacaoService(PerfilFinanceiroService perfilService) {
        this.perfilService = perfilService;
    }

    public SituacaoResponse avaliar(Long usuarioId) {
        PerfilFinanceiro perfil = perfilService.montar(usuarioId);

        BigDecimal totalParcelas = perfil.totalParcelas();
        BigDecimal comprometimento = AnaliseFinanceira.comprometimento(totalParcelas, perfil.rendaMedia());
        SituacaoFinanceira situacao = AnaliseFinanceira.classificar(comprometimento, perfil.dividasAtivas());
        NotaSaude nota = AnaliseFinanceira.calcularNota(comprometimento, perfil.dividasAtivas(),
                perfil.rendaMedia(), perfil.gastosNaoEssenciaisUltimoMes(), perfil.saldoContas(), perfil.totalCofres());

        List<String> avisos = new ArrayList<>();
        if (!perfil.temRenda()) {
            avisos.add(AVISO_SEM_RENDA);
        }

        int atrasadas = (int) perfil.dividasAtivas().stream().filter(AnaliseFinanceira::atrasada).count();

        return new SituacaoResponse(
                situacao.name(),
                situacao.getTitulo(),
                situacao.getOrientacao(),
                nota.nota(),
                perfil.rendaMedia(),
                perfil.origemRenda(),
                perfil.gastosEssenciaisMedia(),
                totalParcelas,
                comprometimento,
                perfil.totalDividas(),
                perfil.dividasAtivas().size(),
                atrasadas,
                perfil.saldoContas(),
                perfil.totalCofres(),
                nota.itens(),
                AnaliseFinanceira.COMO_CALCULAMOS_A_NOTA,
                avisos);
    }
}
