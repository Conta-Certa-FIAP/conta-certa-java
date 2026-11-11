package br.com.fiap.contacerta.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.contacerta.dto.transacao.CategoriaResponse;
import br.com.fiap.contacerta.service.CategoriaService;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    /** GET /categorias - categorias de entrada e saída para o front montar os seletores. */
    @GetMapping
    public List<CategoriaResponse> listar() {
        return categoriaService.listar();
    }
}
