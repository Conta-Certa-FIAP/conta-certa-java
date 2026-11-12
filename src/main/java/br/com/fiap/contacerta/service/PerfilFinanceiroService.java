package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.CofreDAO;
import br.com.fiap.contacerta.dao.ContaBancariaDAO;
import br.com.fiap.contacerta.dao.DividaDAO;
import br.com.fiap.contacerta.dao.TotalCategoria;
import br.com.fiap.contacerta.dao.TransacaoDAO;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.Usuario;

/**
 * Busca no banco os dados que as análises precisam e monta o PerfilFinanceiro.
 * Aqui fica só a "coleta"; as contas ficam em AnaliseFinanceira.
 */
@Service
public class PerfilFinanceiroService {

    private final UsuarioService usuarioService;
    private final TransacaoDAO transacaoDAO;
    private final DividaDAO dividaDAO;
    private final ContaBancariaDAO contaDAO;
    private final CofreDAO cofreDAO;

    public PerfilFinanceiroService(UsuarioService usuarioService, TransacaoDAO transacaoDAO, DividaDAO dividaDAO,
                                   ContaBancariaDAO contaDAO, CofreDAO cofreDAO) {
        this.usuarioService = usuarioService;
        this.transacaoDAO = transacaoDAO;
        this.dividaDAO = dividaDAO;
        this.contaDAO = contaDAO;
        this.cofreDAO = cofreDAO;
    }

    public PerfilFinanceiro montar(Long usuarioId) {
        Usuario usuario = usuarioService.buscarOuFalhar(usuarioId); // 404 se não existir
        LocalDate hoje = LocalDate.now();

        // Janelas móveis: últimos 3 meses (médias) e último mês (gastos não essenciais).
        Periodo tresMeses = Periodo.ultimosMeses(hoje, AnaliseFinanceira.MESES_DA_MEDIA);
        Periodo umMes = Periodo.ultimosMeses(hoje, 1);

        List<TotalCategoria> totaisTresMeses =
                transacaoDAO.totaisPorCategoria(usuarioId, tresMeses.inicio(), tresMeses.fimExclusivo());
        List<TotalCategoria> totaisUmMes =
                transacaoDAO.totaisPorCategoria(usuarioId, umMes.inicio(), umMes.fimExclusivo());
        LocalDate primeiraTransacao = transacaoDAO.primeiraDataDesde(usuarioId, tresMeses.inicio()).orElse(null);
        int meses = AnaliseFinanceira.mesesDeHistorico(primeiraTransacao, hoje);

        // Renda: 1º a média das entradas do extrato; se não houver, a renda declarada no cadastro.
        BigDecimal rendaMedia = AnaliseFinanceira.rendaMedia(totaisTresMeses, meses);
        String origemRenda = "EXTRATO";
        if (rendaMedia.signum() == 0) {
            BigDecimal declarada = usuario.getRendaDeclarada();
            boolean temDeclarada = declarada != null && declarada.signum() > 0;
            rendaMedia = temDeclarada ? declarada : null;
            origemRenda = temDeclarada ? "DECLARADA" : null;
        }

        List<Divida> dividasAtivas = dividaDAO.listarPorUsuario(usuarioId).stream().filter(Divida::ativa).toList();

        return new PerfilFinanceiro(
                usuario,
                rendaMedia,
                origemRenda,
                AnaliseFinanceira.gastosEssenciaisMedia(totaisTresMeses, meses),
                AnaliseFinanceira.naoEssenciais(totaisUmMes),
                contaDAO.somarSaldoPorUsuario(usuarioId),
                cofreDAO.somarGuardadoPorUsuario(usuarioId),
                dividasAtivas);
    }
}
