package br.com.fiap.contacerta.handler;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import br.com.fiap.contacerta.dto.erro.CampoErro;
import br.com.fiap.contacerta.dto.erro.ErroResponse;
import br.com.fiap.contacerta.exception.BancoDeDadosException;
import br.com.fiap.contacerta.exception.CredenciaisInvalidasException;
import br.com.fiap.contacerta.exception.RecursoNaoEncontradoException;
import br.com.fiap.contacerta.exception.RegraNegocioException;
import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;
import br.com.fiap.contacerta.exception.ServicoIndisponivelException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Tratamento GLOBAL de erros.
 *
 * O @RestControllerAdvice "escuta" as exceções lançadas por qualquer controller/service/DAO.
 * Cada método @ExceptionHandler abaixo escolhe o status HTTP certo e devolve sempre o mesmo
 * JSON (ErroResponse). Assim nenhum controller precisa de try/catch e o usuário nunca
 * recebe stack trace, SQL ou página HTML.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** ORA-00001: tentativa de gravar valor repetido em coluna UNIQUE. */
    private static final int ORA_VALOR_DUPLICADO = 1;
    /** ORA-02292: tentativa de apagar um registro que ainda tem "filhos" (chave estrangeira). */
    private static final int ORA_REGISTRO_COM_FILHOS = 2292;

    // ------------------------------------------------------------------ 400

    /** Bean Validation reprovou o corpo (@Valid): devolve a lista de campos com problema. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<CampoErro> campos = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.add(new CampoErro(erro.getField(), erro.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(erro -> campos.add(new CampoErro(erro.getObjectName(), erro.getDefaultMessage())));
        return responder(HttpStatus.BAD_REQUEST, "Dados inválidos. Confira os campos informados.", request, campos);
    }

    /** Validação de parâmetros de rota/consulta (quando houver anotações neles). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErroResponse> validacaoDeParametro(HandlerMethodValidationException ex,
                                                             HttpServletRequest request) {
        List<CampoErro> campos = new ArrayList<>();
        ex.getParameterValidationResults().forEach(resultado -> {
            String nome = resultado.getMethodParameter().getParameterName();
            resultado.getResolvableErrors()
                    .forEach(erro -> campos.add(new CampoErro(nome, erro.getDefaultMessage())));
        });
        return responder(HttpStatus.BAD_REQUEST, "Dados inválidos. Confira os campos informados.", request, campos);
    }

    /** JSON quebrado, data fora do formato AAAA-MM-DD, texto onde devia ser número... */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST,
                "JSON inválido ou mal formatado. Confira aspas, vírgulas, números e datas (AAAA-MM-DD).",
                request, null);
    }

    /** Ex.: GET /usuarios/abc (o id precisa ser número). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tipoInvalido(MethodArgumentTypeMismatchException ex,
                                                     HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST,
                "O parâmetro '" + ex.getName() + "' tem um valor inválido.", request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> parametroFaltando(MissingServletRequestParameterException ex,
                                                          HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST,
                "O parâmetro '" + ex.getParameterName() + "' é obrigatório.", request, null);
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ErroResponse> requisicaoInvalida(RequisicaoInvalidaException ex,
                                                           HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    // ------------------------------------------------------------------ 401

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> loginInvalido(CredenciaisInvalidasException ex, HttpServletRequest request) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    // ------------------------------------------------------------------ 404

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    /** Endereço que não existe na API. */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErroResponse> rotaInexistente(Exception ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, "Rota não encontrada.", request, null);
    }

    // ------------------------------------------------------------------ 405 e 415

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodoNaoSuportado(HttpRequestMethodNotSupportedException ex,
                                                           HttpServletRequest request) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED,
                "Método " + ex.getMethod() + " não é suportado nesta rota.", request, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> tipoDeConteudo(HttpMediaTypeNotSupportedException ex,
                                                       HttpServletRequest request) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Envie o corpo em JSON (header Content-Type: application/json).", request, null);
    }

    // ------------------------------------------------------------------ 409

    /** Regras de negócio (inclui SaldoInsuficienteException e LimiteInsuficienteException, que são filhas). */
    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraDeNegocio(RegraNegocioException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // ------------------------------------------------------------------ 503

    @ExceptionHandler(ServicoIndisponivelException.class)
    public ResponseEntity<ErroResponse> servicoIndisponivel(ServicoIndisponivelException ex,
                                                            HttpServletRequest request) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request, null);
    }

    // ------------------------------------------------------------------ 500

    /**
     * Erro vindo do banco. Os services já validam antes de gravar, mas se mesmo assim o Oracle
     * recusar por UNIQUE ou por chave estrangeira, respondemos 409 em vez de 500.
     * O detalhe técnico vai só para o log do servidor.
     */
    @ExceptionHandler(BancoDeDadosException.class)
    public ResponseEntity<ErroResponse> bancoDeDados(BancoDeDadosException ex, HttpServletRequest request) {
        if (ex.getCodigoOracle() == ORA_VALOR_DUPLICADO) {
            return responder(HttpStatus.CONFLICT, "Já existe um registro com esses dados.", request, null);
        }
        if (ex.getCodigoOracle() == ORA_REGISTRO_COM_FILHOS) {
            return responder(HttpStatus.CONFLICT,
                    "Este registro possui outros registros ligados a ele e não pode ser removido.", request, null);
        }
        log.error("Erro de banco de dados em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente em instantes.", request, null);
    }

    /** Qualquer erro não previsto: mensagem genérica para o usuário, detalhe só no log. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> erroInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente em instantes.", request, null);
    }

    // ------------------------------------------------------------------ apoio

    private ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem,
                                                   HttpServletRequest request, List<CampoErro> campos) {
        ErroResponse corpo = ErroResponse.de(status, mensagem, request.getRequestURI(), campos);
        return ResponseEntity.status(status).body(corpo);
    }
}
