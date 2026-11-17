package br.com.fiap.contacerta.dto.erro;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Formato ÚNICO de erro da API. Todo erro sai assim:
 * {"timestamp": "...", "status": 409, "erro": "Conflito", "mensagem": "...", "caminho": "/dividas/1"}
 * Em erro de validação vem também a lista "campos" (campo + mensagem).
 *
 * É um record: classe imutável só de dados (o Java gera construtor e leitura dos campos).
 * O @JsonInclude faz o "campos" sumir do JSON quando for nulo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoErro> campos) {

    /**
     * O front lê o texto do erro no campo "message" (ver ENDPOINTS.md do front).
     * Por isso o JSON leva a mesma mensagem duas vezes: em "mensagem" e em "message".
     */
    @JsonProperty("message")
    public String message() {
        return mensagem;
    }

    /** Monta o erro a partir do status HTTP, já com o nome do erro em português. */
    public static ErroResponse de(HttpStatus status, String mensagem, String caminho, List<CampoErro> campos) {
        LocalDateTime agora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        return new ErroResponse(agora, status.value(), nomeDoErro(status), mensagem, caminho, campos);
    }

    private static String nomeDoErro(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Requisição inválida";
            case UNAUTHORIZED -> "Não autorizado";
            case FORBIDDEN -> "Acesso negado";
            case TOO_MANY_REQUESTS -> "Muitas requisições";
            case NOT_FOUND -> "Não encontrado";
            case METHOD_NOT_ALLOWED -> "Método não permitido";
            case CONFLICT -> "Conflito";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de conteúdo não suportado";
            case SERVICE_UNAVAILABLE -> "Serviço indisponível";
            case INTERNAL_SERVER_ERROR -> "Erro interno";
            default -> status.getReasonPhrase();
        };
    }
}
