package br.com.fiap.contacerta.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import br.com.fiap.contacerta.dto.front.ContactMessageDto;
import br.com.fiap.contacerta.dto.front.DashboardDto;
import br.com.fiap.contacerta.dto.front.DebtDetailDto;
import br.com.fiap.contacerta.dto.front.DebtDto;
import br.com.fiap.contacerta.dto.front.DebtInputDto;
import br.com.fiap.contacerta.dto.front.LearningTrackDto;
import br.com.fiap.contacerta.dto.front.NewTransactionDto;
import br.com.fiap.contacerta.dto.front.PaymentInputDto;
import br.com.fiap.contacerta.dto.front.PlanDto;
import br.com.fiap.contacerta.dto.front.PlanUpdateDto;
import br.com.fiap.contacerta.dto.front.TransactionDto;
import br.com.fiap.contacerta.seguranca.AutenticacaoInterceptor;
import br.com.fiap.contacerta.service.front.ContatoService;
import br.com.fiap.contacerta.service.front.DashboardService;
import br.com.fiap.contacerta.service.front.DividaFrontService;
import br.com.fiap.contacerta.service.front.PlanoFrontService;
import br.com.fiap.contacerta.service.front.TransacaoFrontService;
import br.com.fiap.contacerta.service.front.TrilhaFrontService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * As rotas que o front chama (contrato do arquivo ENDPOINTS.md do front).
 *
 * Diferença em relação às rotas em português: aqui o id do usuário NUNCA vem na URL.
 * Quem diz de quem são os dados é o token do login: o AutenticacaoInterceptor confere o
 * token e coloca o id do usuário no atributo "usuarioId" da requisição, que é lido abaixo
 * com @RequestAttribute.
 */
@RestController
public class FrontController {

    private static final String USUARIO = AutenticacaoInterceptor.USUARIO_ID;

    private final DashboardService dashboardService;
    private final TransacaoFrontService transacaoService;
    private final DividaFrontService dividaService;
    private final PlanoFrontService planoService;
    private final TrilhaFrontService trilhaService;
    private final ContatoService contatoService;

    public FrontController(DashboardService dashboardService, TransacaoFrontService transacaoService,
                           DividaFrontService dividaService, PlanoFrontService planoService,
                           TrilhaFrontService trilhaService, ContatoService contatoService) {
        this.dashboardService = dashboardService;
        this.transacaoService = transacaoService;
        this.dividaService = dividaService;
        this.planoService = planoService;
        this.trilhaService = trilhaService;
        this.contatoService = contatoService;
    }

    // ------------------------------------------------------------------ site público

    /** POST /contact - rota pública (não exige login). 204 = recebido, sem corpo. */
    @PostMapping("/contact")
    public ResponseEntity<Void> contato(@Valid @RequestBody ContactMessageDto mensagem, HttpServletRequest request) {
        contatoService.registrar(mensagem, origem(request));
        return ResponseEntity.noContent().build();
    }

    /**
     * IP de quem enviou. No Render a requisição passa por um proxy, que coloca o IP original
     * no cabeçalho X-Forwarded-For (o primeiro da lista); rodando no computador ele não existe.
     */
    private static String origem(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ------------------------------------------------------------------ início

    @GetMapping("/dashboard")
    public DashboardDto dashboard(@RequestAttribute(USUARIO) Long usuarioId,
                                  @RequestParam(required = false) String month) {
        return dashboardService.montar(usuarioId, month);
    }

    // ------------------------------------------------------------------ transações

    @GetMapping("/transactions")
    public List<TransactionDto> transacoes(@RequestAttribute(USUARIO) Long usuarioId,
                                           @RequestParam(required = false) String month) {
        return transacaoService.listar(usuarioId, month);
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionDto> lancar(@RequestAttribute(USUARIO) Long usuarioId,
                                                 @Valid @RequestBody NewTransactionDto entrada,
                                                 UriComponentsBuilder uriBuilder) {
        TransactionDto criada = transacaoService.criar(usuarioId, entrada);
        URI location = uriBuilder.path("/transacoes/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    // ------------------------------------------------------------------ dívidas

    @GetMapping("/debts")
    public List<DebtDto> dividas(@RequestAttribute(USUARIO) Long usuarioId) {
        return dividaService.listar(usuarioId);
    }

    @GetMapping("/debts/{id}")
    public DebtDetailDto divida(@RequestAttribute(USUARIO) Long usuarioId, @PathVariable Long id) {
        return dividaService.buscar(usuarioId, id);
    }

    @PostMapping("/debts")
    public ResponseEntity<DebtDto> cadastrarDivida(@RequestAttribute(USUARIO) Long usuarioId,
                                                   @Valid @RequestBody DebtInputDto entrada,
                                                   UriComponentsBuilder uriBuilder) {
        DebtDto criada = dividaService.criar(usuarioId, entrada);
        URI location = uriBuilder.path("/debts/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping("/debts/{id}")
    public DebtDto editarDivida(@RequestAttribute(USUARIO) Long usuarioId, @PathVariable Long id,
                                @Valid @RequestBody DebtInputDto entrada) {
        return dividaService.atualizar(usuarioId, id, entrada);
    }

    @DeleteMapping("/debts/{id}")
    public ResponseEntity<Void> excluirDivida(@RequestAttribute(USUARIO) Long usuarioId, @PathVariable Long id) {
        dividaService.remover(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    /** Rota nova (não estava no ENDPOINTS.md): botão "Registrar pagamento" do detalhe da dívida. */
    @PostMapping("/debts/{id}/payments")
    public DebtDetailDto pagar(@RequestAttribute(USUARIO) Long usuarioId, @PathVariable Long id,
                               @Valid @RequestBody PaymentInputDto entrada) {
        return dividaService.pagar(usuarioId, id, entrada);
    }

    // ------------------------------------------------------------------ plano

    @GetMapping("/plan")
    public PlanDto plano(@RequestAttribute(USUARIO) Long usuarioId) {
        return planoService.buscar(usuarioId);
    }

    @PutMapping("/plan")
    public PlanDto salvarPlano(@RequestAttribute(USUARIO) Long usuarioId, @Valid @RequestBody PlanUpdateDto entrada) {
        return planoService.salvar(usuarioId, entrada);
    }

    // ------------------------------------------------------------------ aprender

    @GetMapping("/learning/tracks")
    public List<LearningTrackDto> trilhas(@RequestAttribute(USUARIO) Long usuarioId) {
        return trilhaService.listar(usuarioId);
    }

    @PostMapping("/learning/tracks/{trackId}/lessons/{lessonId}/complete")
    public LearningTrackDto concluirLicao(@RequestAttribute(USUARIO) Long usuarioId,
                                          @PathVariable Long trackId, @PathVariable Long lessonId) {
        return trilhaService.concluir(usuarioId, trackId, lessonId);
    }
}
