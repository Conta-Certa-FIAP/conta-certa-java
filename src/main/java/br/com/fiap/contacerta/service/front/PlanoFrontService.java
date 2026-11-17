package br.com.fiap.contacerta.service.front;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.PlanoDAO;
import br.com.fiap.contacerta.dto.front.PlanDto;
import br.com.fiap.contacerta.dto.front.PlanUpdateDto;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.PlanoSalvo;
import br.com.fiap.contacerta.service.ConsultoriaService;
import br.com.fiap.contacerta.service.PerfilFinanceiro;
import br.com.fiap.contacerta.service.PerfilFinanceiroService;
import br.com.fiap.contacerta.service.PlanoQuitacaoCalculadora.Metodo;

/** Rotas GET /plan e PUT /plan. */
@Service
public class PlanoFrontService {

    private final PlanoDAO planoDAO;
    private final PerfilFinanceiroService perfilService;

    public PlanoFrontService(PlanoDAO planoDAO, PerfilFinanceiroService perfilService) {
        this.planoDAO = planoDAO;
        this.perfilService = perfilService;
    }

    /**
     * O plano salvo pela pessoa. Se ela ainda não salvou nenhum, sugerimos um: o valor da
     * consultoria (o menor entre a folga e 40% da renda), mas nunca menos que a soma das parcelas,
     * e o método avalanche.
     */
    public PlanDto buscar(Long usuarioId) {
        PerfilFinanceiro perfil = perfilService.montar(usuarioId); // 404 se o usuário não existir
        PlanoSalvo salvo = planoDAO.buscarPorUsuario(usuarioId).orElse(null);

        BigDecimal valor = salvo != null ? salvo.valorMensal() : valorSugerido(perfil);
        Metodo metodo = salvo != null ? Metodo.valueOf(salvo.metodo()) : Metodo.AVALANCHE;
        return PlanoFront.montar(perfil.dividasAtivas(), valor, metodo, YearMonth.now());
    }

    public PlanDto salvar(Long usuarioId, PlanUpdateDto entrada) {
        PerfilFinanceiro perfil = perfilService.montar(usuarioId);
        BigDecimal valor = entrada.monthlyBudget().setScale(2, RoundingMode.HALF_UP);
        planoDAO.salvar(new PlanoSalvo(usuarioId, valor, entrada.method()));
        return PlanoFront.montar(perfil.dividasAtivas(), valor, Metodo.valueOf(entrada.method()), YearMonth.now());
    }

    /** Usado pelo dashboard para o cartão "Meu plano" (nulo sem dívidas ou se o plano não quita tudo). */
    public PlanDto paraDashboard(PerfilFinanceiro perfil, Long usuarioId) {
        List<Divida> dividas = perfil.dividasAtivas();
        if (dividas.isEmpty()) {
            return null;
        }
        PlanoSalvo salvo = planoDAO.buscarPorUsuario(usuarioId).orElse(null);
        BigDecimal valor = salvo != null ? salvo.valorMensal() : valorSugerido(perfil);
        Metodo metodo = salvo != null ? Metodo.valueOf(salvo.metodo()) : Metodo.AVALANCHE;
        return PlanoFront.montar(dividas, valor, metodo, YearMonth.now());
    }

    static BigDecimal valorSugerido(PerfilFinanceiro perfil) {
        return ConsultoriaService.valorSugerido(perfil).max(perfil.totalParcelas()).setScale(2, RoundingMode.HALF_UP);
    }
}
