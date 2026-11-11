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

import br.com.fiap.contacerta.dto.conta.ContaAtualizacaoRequest;
import br.com.fiap.contacerta.dto.conta.ContaRequest;
import br.com.fiap.contacerta.dto.conta.ContaResponse;
import br.com.fiap.contacerta.service.ContaService;
import jakarta.validation.Valid;

@RestController
public class ContaController {

    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
    }

    @GetMapping("/usuarios/{usuarioId}/contas")
    public List<ContaResponse> listar(@PathVariable Long usuarioId) {
        return contaService.listarPorUsuario(usuarioId);
    }

    /** Conecta um banco. Com importarDadosSimulados = true, gera cartões, extrato e dívidas. */
    @PostMapping("/usuarios/{usuarioId}/contas")
    public ResponseEntity<ContaResponse> criar(@PathVariable Long usuarioId,
                                               @Valid @RequestBody ContaRequest request,
                                               UriComponentsBuilder uriBuilder) {
        ContaResponse criada = contaService.criar(usuarioId, request);
        URI location = uriBuilder.path("/contas/{id}").buildAndExpand(criada.getId()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping("/contas/{id}")
    public ContaResponse buscar(@PathVariable Long id) {
        return contaService.buscar(id);
    }

    @PutMapping("/contas/{id}")
    public ContaResponse atualizar(@PathVariable Long id, @Valid @RequestBody ContaAtualizacaoRequest request) {
        return contaService.atualizar(id, request);
    }

    @DeleteMapping("/contas/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        contaService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
