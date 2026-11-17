package br.com.fiap.contacerta.seguranca;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import br.com.fiap.contacerta.exception.NaoAutenticadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * "Porteiro" da API: roda ANTES de qualquer controller.
 *
 *  1. Rotas públicas (login, cadastro, contato, catálogos) passam direto.
 *  2. Nas outras, exige o cabeçalho "Authorization: Bearer <token>" e confere o token (401 se falhar).
 *  3. Guarda o id do usuário na requisição, para o controller ler com
 *     {@code @RequestAttribute(AutenticacaoInterceptor.USUARIO_ID)}.
 *  4. Se a rota tem um id na URL (/contas/{id}/...), confere se o registro é de quem está logado (403).
 */
@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {

    /** Nome do atributo da requisição onde fica o id do usuário logado. */
    public static final String USUARIO_ID = "usuarioId";

    private static final String PREFIXO = "Bearer ";

    /** Rotas que funcionam sem login (comparadas com o "molde" da rota, ex.: /indicadores/historico). */
    private static final Set<String> ROTAS_PUBLICAS = Set.of(
            "/health",
            "/auth/login",
            "/auth/cadastro",
            "/contact",
            "/bancos",
            "/categorias",
            "/indicadores",
            "/indicadores/historico");

    private final TokenService tokenService;
    private final DonoDoRecurso donoDoRecurso;

    public AutenticacaoInterceptor(TokenService tokenService, DonoDoRecurso donoDoRecurso) {
        this.tokenService = tokenService;
        this.donoDoRecurso = donoDoRecurso;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS é a "pergunta" de CORS que o navegador faz antes da requisição de verdade: nunca tem token.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // O Spring já descobriu qual rota casou; usamos o molde dela (ex.: "/contas/{contaId}/cartoes")
        // em vez do texto cru da URL, que poderia vir escrito de formas diferentes.
        String molde = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (molde != null && ROTAS_PUBLICAS.contains(molde)) {
            return true;
        }

        String cabecalho = request.getHeader("Authorization");
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO)) {
            throw new NaoAutenticadoException("Faça login para continuar.");
        }
        Long usuarioId = tokenService.validar(cabecalho.substring(PREFIXO.length()).trim());
        request.setAttribute(USUARIO_ID, usuarioId);

        conferirDono(request, molde, usuarioId);
        return true;
    }

    /** Em rotas como "/dividas/{id}/pagamentos": recurso = "dividas" e o id é a 1ª variável do molde. */
    private void conferirDono(HttpServletRequest request, String molde, Long usuarioId) {
        if (molde == null) {
            return;
        }
        String[] partes = molde.split("/"); // "/dividas/{id}/pagamentos" -> ["", "dividas", "{id}", "pagamentos"]
        if (partes.length < 3 || !partes[2].startsWith("{") || !partes[2].endsWith("}")) {
            return;
        }
        String nomeDaVariavel = partes[2].substring(1, partes[2].length() - 1);

        @SuppressWarnings("unchecked")
        Map<String, String> variaveis =
                (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variaveis == null || variaveis.get(nomeDaVariavel) == null) {
            return;
        }
        try {
            Long id = Long.valueOf(variaveis.get(nomeDaVariavel));
            donoDoRecurso.conferir(partes[1], id, usuarioId);
        } catch (NumberFormatException e) {
            // id que não é número: o controller responde 400 logo em seguida
        }
    }
}
