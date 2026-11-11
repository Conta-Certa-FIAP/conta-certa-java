package br.com.fiap.contacerta.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import br.com.fiap.contacerta.dto.usuario.LoginRequest;
import br.com.fiap.contacerta.dto.usuario.LoginResponse;
import br.com.fiap.contacerta.dto.usuario.UsuarioCadastroRequest;
import br.com.fiap.contacerta.dto.usuario.UsuarioResponse;
import br.com.fiap.contacerta.service.UsuarioService;
import jakarta.validation.Valid;

/** Cadastro e login. O controller só recebe/devolve HTTP; as regras ficam no service. */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** POST /auth/cadastro -> 201 + header Location apontando para o usuário criado. */
    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody UsuarioCadastroRequest request,
                                                     UriComponentsBuilder uriBuilder) {
        UsuarioResponse criado = usuarioService.cadastrar(request);
        URI location = uriBuilder.path("/usuarios/{id}").buildAndExpand(criado.getId()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    /** POST /auth/login -> 200 com os dados básicos, ou 401 com mensagem genérica. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return usuarioService.login(request);
    }
}
