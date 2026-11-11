package br.com.fiap.contacerta.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import br.com.fiap.contacerta.dto.transacao.SimularCompraRequest;
import br.com.fiap.contacerta.dto.transacao.TransacaoRequest;
import br.com.fiap.contacerta.dto.transacao.TransacaoResponse;
import br.com.fiap.contacerta.service.TransacaoService;
import jakarta.validation.Valid;

@RestController
public class TransacaoController {

    private final TransacaoService transacaoService;

    public TransacaoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    /** Extrato de uma conta. Filtros opcionais: ?mes=AAAA-MM&tipo=ENTRADA|SAIDA&categoria=ID */
    @GetMapping("/contas/{contaId}/transacoes")
    public List<TransacaoResponse> listarPorConta(@PathVariable Long contaId,
                                                  @RequestParam(required = false) String mes,
                                                  @RequestParam(required = false) String tipo,
                                                  @RequestParam(required = false) Long categoria) {
        return transacaoService.listarPorConta(contaId, mes, tipo, categoria);
    }

    /** Extrato de todas as contas do usuário, com os mesmos filtros. */
    @GetMapping("/usuarios/{usuarioId}/transacoes")
    public List<TransacaoResponse> listarPorUsuario(@PathVariable Long usuarioId,
                                                    @RequestParam(required = false) String mes,
                                                    @RequestParam(required = false) String tipo,
                                                    @RequestParam(required = false) Long categoria) {
        return transacaoService.listarPorUsuario(usuarioId, mes, tipo, categoria);
    }

    @GetMapping("/transacoes/{id}")
    public TransacaoResponse buscar(@PathVariable Long id) {
        return transacaoService.buscar(id);
    }

    /** Lançamento manual de entrada ou saída. */
    @PostMapping("/contas/{contaId}/transacoes")
    public ResponseEntity<TransacaoResponse> lancar(@PathVariable Long contaId,
                                                    @Valid @RequestBody TransacaoRequest request,
                                                    UriComponentsBuilder uriBuilder) {
        TransacaoResponse criada = transacaoService.lancar(contaId, request);
        return ResponseEntity.created(location(uriBuilder, criada)).body(criada);
    }

    /** Botão "simular compra": cria uma compra aleatória no cartão de crédito ou débito. */
    @PostMapping("/contas/{contaId}/transacoes/simular-compra")
    public ResponseEntity<TransacaoResponse> simularCompra(@PathVariable Long contaId,
                                                           @Valid @RequestBody SimularCompraRequest request,
                                                           UriComponentsBuilder uriBuilder) {
        TransacaoResponse criada = transacaoService.simularCompra(contaId, request);
        return ResponseEntity.created(location(uriBuilder, criada)).body(criada);
    }

    @DeleteMapping("/transacoes/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        transacaoService.remover(id);
        return ResponseEntity.noContent().build();
    }

    private URI location(UriComponentsBuilder uriBuilder, TransacaoResponse transacao) {
        return uriBuilder.path("/transacoes/{id}").buildAndExpand(transacao.getId()).toUri();
    }
}
