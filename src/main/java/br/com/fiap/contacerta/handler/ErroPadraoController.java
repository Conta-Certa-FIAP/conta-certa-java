package br.com.fiap.contacerta.handler;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.contacerta.dto.erro.ErroResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Rede de segurança: se um erro acontecer FORA dos controllers (antes de chegar no
 * GlobalExceptionHandler), o Tomcat encaminha para "/error". O padrão do Spring seria
 * devolver uma página HTML ("Whitelabel Error Page"); aqui devolvemos o mesmo JSON padronizado.
 */
@RestController
public class ErroPadraoController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ErroResponse> erro(HttpServletRequest request) {
        Object codigo = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object caminho = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (codigo instanceof Integer numero && HttpStatus.resolve(numero) != null) {
            status = HttpStatus.valueOf(numero);
        }

        String mensagem = switch (status) {
            case NOT_FOUND -> "Rota não encontrada.";
            case METHOD_NOT_ALLOWED -> "Método HTTP não suportado nesta rota.";
            case BAD_REQUEST -> "Requisição inválida.";
            default -> "Erro interno. Tente novamente em instantes.";
        };

        String rota = caminho != null ? caminho.toString() : request.getRequestURI();
        return ResponseEntity.status(status).body(ErroResponse.de(status, mensagem, rota, null));
    }
}
