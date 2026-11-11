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

import br.com.fiap.contacerta.dto.conta.CartaoAtualizacaoRequest;
import br.com.fiap.contacerta.dto.conta.CartaoRequest;
import br.com.fiap.contacerta.dto.conta.CartaoResponse;
import br.com.fiap.contacerta.service.CartaoService;
import jakarta.validation.Valid;

@RestController
public class CartaoController {

    private final CartaoService cartaoService;

    public CartaoController(CartaoService cartaoService) {
        this.cartaoService = cartaoService;
    }

    @GetMapping("/contas/{contaId}/cartoes")
    public List<CartaoResponse> listar(@PathVariable Long contaId) {
        return cartaoService.listarPorConta(contaId);
    }

    @PostMapping("/contas/{contaId}/cartoes")
    public ResponseEntity<CartaoResponse> criar(@PathVariable Long contaId,
                                                @Valid @RequestBody CartaoRequest request,
                                                UriComponentsBuilder uriBuilder) {
        CartaoResponse criado = cartaoService.criar(contaId, request);
        URI location = uriBuilder.path("/cartoes/{id}").buildAndExpand(criado.getId()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping("/cartoes/{id}")
    public CartaoResponse buscar(@PathVariable Long id) {
        return cartaoService.buscar(id);
    }

    @PutMapping("/cartoes/{id}")
    public CartaoResponse atualizar(@PathVariable Long id, @Valid @RequestBody CartaoAtualizacaoRequest request) {
        return cartaoService.atualizar(id, request);
    }

    @DeleteMapping("/cartoes/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        cartaoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
