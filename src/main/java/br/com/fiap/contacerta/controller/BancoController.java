package br.com.fiap.contacerta.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.contacerta.dto.conta.BancoResponse;
import br.com.fiap.contacerta.service.BancoService;

@RestController
@RequestMapping("/bancos")
public class BancoController {

    private final BancoService bancoService;

    public BancoController(BancoService bancoService) {
        this.bancoService = bancoService;
    }

    /** GET /bancos - catálogo de bancos para o usuário escolher ao conectar uma conta. */
    @GetMapping
    public List<BancoResponse> listar() {
        return bancoService.listar();
    }
}
