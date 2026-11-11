package br.com.fiap.contacerta.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.contacerta.dto.usuario.UsuarioAtualizacaoRequest;
import br.com.fiap.contacerta.dto.usuario.UsuarioResponse;
import br.com.fiap.contacerta.service.UsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return usuarioService.buscar(id);
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioAtualizacaoRequest request) {
        return usuarioService.atualizar(id, request);
    }

    /** 204 = deu certo e não há corpo na resposta. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        usuarioService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
