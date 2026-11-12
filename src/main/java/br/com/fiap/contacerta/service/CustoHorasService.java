package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dto.analise.CustoEmHorasResponse;
import br.com.fiap.contacerta.exception.RegraNegocioException;
import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;

/**
 * "Custo em horas de trabalho": traduz um preço em tempo de vida.
 *   valor da hora = renda média / horas trabalhadas por mês
 *   horas         = valor da compra / valor da hora
 */
@Service
public class CustoHorasService {

    private final PerfilFinanceiroService perfilService;

    public CustoHorasService(PerfilFinanceiroService perfilService) {
        this.perfilService = perfilService;
    }

    public CustoEmHorasResponse calcular(Long usuarioId, BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new RequisicaoInvalidaException("O parâmetro 'valor' deve ser maior que zero.");
        }
        PerfilFinanceiro perfil = perfilService.montar(usuarioId);
        if (!perfil.temRenda()) {
            throw new RegraNegocioException("Não conseguimos calcular: informe sua renda mensal no cadastro "
                    + "ou registre suas entradas.");
        }
        return calcular(valor, perfil.rendaMedia(), perfil.usuario().getHorasTrabalhoMes());
    }

    /** A conta em si, sem banco de dados (ver CustoHorasServiceTest). */
    static CustoEmHorasResponse calcular(BigDecimal valor, BigDecimal rendaMedia, int horasTrabalhoMes) {
        BigDecimal valorHora = rendaMedia.divide(BigDecimal.valueOf(horasTrabalhoMes), 2, RoundingMode.HALF_UP);
        // Divide pela renda "cheia" (e não pelo valor da hora arredondado) para não acumular erro.
        double horas = valor.doubleValue() * horasTrabalhoMes / rendaMedia.doubleValue();
        String formatadas = Formatador.horas(horas);

        return new CustoEmHorasResponse(
                valor,
                rendaMedia,
                horasTrabalhoMes,
                valorHora,
                CalculadoraFinanceira.arredondar(horas),
                formatadas,
                "Essa compra custa " + formatadas + " do seu trabalho");
    }
}
