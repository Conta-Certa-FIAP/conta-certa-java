package br.com.fiap.contacerta.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import br.com.fiap.contacerta.dto.divida.DividaRequest;
import br.com.fiap.contacerta.dto.divida.DividaResponse;
import br.com.fiap.contacerta.dto.divida.PagamentoRequest;
import br.com.fiap.contacerta.dto.divida.PagamentoResponse;
import br.com.fiap.contacerta.service.DividaService;
import br.com.fiap.contacerta.service.PagamentoService;
import jakarta.validation.Valid;

/** Dívidas e seus pagamentos. */
@RestController
public class DividaController {

    private final DividaService dividaService;
    private final PagamentoService pagamentoService;

    public DividaController(DividaService dividaService, PagamentoService pagamentoService) {
        this.dividaService = dividaService;
        this.pagamentoService = pagamentoService;
    }

    @GetMapping("/usuarios/{usuarioId}/dividas")
    public List<DividaResponse> listar(@PathVariable Long usuarioId) {
        return dividaService.listarPorUsuario(usuarioId);
    }

    @PostMapping("/usuarios/{usuarioId}/dividas")
    public ResponseEntity<DividaResponse> criar(@PathVariable Long usuarioId,
                                                @Valid @RequestBody DividaRequest request,
                                                UriComponentsBuilder uriBuilder) {
        DividaResponse criada = dividaService.criar(usuarioId, request);
        URI location = uriBuilder.path("/dividas/{id}").buildAndExpand(criada.getId()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping("/dividas/{id}")
    public DividaResponse buscar(@PathVariable Long id) {
        return dividaService.buscar(id);
    }

    @PutMapping("/dividas/{id}")
    public DividaResponse atualizar(@PathVariable Long id, @Valid @RequestBody DividaRequest request) {
        return dividaService.atualizar(id, request);
    }

    @DeleteMapping("/dividas/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        dividaService.remover(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ pagamentos

    @GetMapping("/dividas/{dividaId}/pagamentos")
    public List<PagamentoResponse> listarPagamentos(@PathVariable Long dividaId) {
        return pagamentoService.listarPorDivida(dividaId);
    }

    /** Registra um pagamento: abate o saldo devedor e, se a conta for informada, o saldo da conta. */
    @PostMapping("/dividas/{dividaId}/pagamentos")
    public ResponseEntity<PagamentoResponse> registrarPagamento(@PathVariable Long dividaId,
                                                                @Valid @RequestBody PagamentoRequest request,
                                                                UriComponentsBuilder uriBuilder) {
        PagamentoResponse criado = pagamentoService.registrar(dividaId, request);
        URI location = uriBuilder.path("/pagamentos/{id}").buildAndExpand(criado.getId()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping("/pagamentos/{id}")
    public PagamentoResponse buscarPagamento(@PathVariable Long id) {
        return pagamentoService.buscar(id);
    }
}
