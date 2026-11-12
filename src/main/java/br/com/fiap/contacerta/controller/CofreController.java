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

import br.com.fiap.contacerta.dto.cofre.CofreRequest;
import br.com.fiap.contacerta.dto.cofre.CofreResponse;
import br.com.fiap.contacerta.dto.cofre.MovimentoCofreRequest;
import br.com.fiap.contacerta.dto.cofre.MovimentoCofreResponse;
import br.com.fiap.contacerta.service.CofreService;
import jakarta.validation.Valid;

/** Cofres (metas de economia), depósitos e resgates. */
@RestController
public class CofreController {

    private final CofreService cofreService;

    public CofreController(CofreService cofreService) {
        this.cofreService = cofreService;
    }

    @GetMapping("/usuarios/{usuarioId}/cofres")
    public List<CofreResponse> listar(@PathVariable Long usuarioId) {
        return cofreService.listarPorUsuario(usuarioId);
    }

    @PostMapping("/usuarios/{usuarioId}/cofres")
    public ResponseEntity<CofreResponse> criar(@PathVariable Long usuarioId,
                                               @Valid @RequestBody CofreRequest request,
                                               UriComponentsBuilder uriBuilder) {
        CofreResponse criado = cofreService.criar(usuarioId, request);
        URI location = uriBuilder.path("/cofres/{id}").buildAndExpand(criado.getId()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping("/cofres/{id}")
    public CofreResponse buscar(@PathVariable Long id) {
        return cofreService.buscar(id);
    }

    @PutMapping("/cofres/{id}")
    public CofreResponse atualizar(@PathVariable Long id, @Valid @RequestBody CofreRequest request) {
        return cofreService.atualizar(id, request);
    }

    @DeleteMapping("/cofres/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        cofreService.remover(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ movimentos

    @GetMapping("/cofres/{cofreId}/movimentos")
    public List<MovimentoCofreResponse> listarMovimentos(@PathVariable Long cofreId) {
        return cofreService.listarMovimentos(cofreId);
    }

    @GetMapping("/cofres/{cofreId}/movimentos/{movimentoId}")
    public MovimentoCofreResponse buscarMovimento(@PathVariable Long cofreId, @PathVariable Long movimentoId) {
        return cofreService.buscarMovimento(cofreId, movimentoId);
    }

    @PostMapping("/cofres/{cofreId}/depositos")
    public ResponseEntity<MovimentoCofreResponse> depositar(@PathVariable Long cofreId,
                                                            @Valid @RequestBody MovimentoCofreRequest request,
                                                            UriComponentsBuilder uriBuilder) {
        MovimentoCofreResponse criado = cofreService.depositar(cofreId, request);
        return ResponseEntity.created(location(uriBuilder, criado)).body(criado);
    }

    @PostMapping("/cofres/{cofreId}/resgates")
    public ResponseEntity<MovimentoCofreResponse> resgatar(@PathVariable Long cofreId,
                                                           @Valid @RequestBody MovimentoCofreRequest request,
                                                           UriComponentsBuilder uriBuilder) {
        MovimentoCofreResponse criado = cofreService.resgatar(cofreId, request);
        return ResponseEntity.created(location(uriBuilder, criado)).body(criado);
    }

    private URI location(UriComponentsBuilder uriBuilder, MovimentoCofreResponse movimento) {
        return uriBuilder.path("/cofres/{cofreId}/movimentos/{id}")
                .buildAndExpand(movimento.getCofreId(), movimento.getId()).toUri();
    }
}
