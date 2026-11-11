package br.com.fiap.contacerta.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.CategoriaDAO;
import br.com.fiap.contacerta.dto.transacao.CategoriaResponse;
import br.com.fiap.contacerta.exception.RecursoNaoEncontradoException;
import br.com.fiap.contacerta.model.Categoria;

/** Categorias de entradas e saídas (só leitura: vêm do script do banco). */
@Service
public class CategoriaService {

    private final CategoriaDAO categoriaDAO;
    private final ModelMapper mapper;

    public CategoriaService(CategoriaDAO categoriaDAO, ModelMapper mapper) {
        this.categoriaDAO = categoriaDAO;
        this.mapper = mapper;
    }

    public List<CategoriaResponse> listar() {
        return categoriaDAO.listar().stream()
                .map(categoria -> mapper.map(categoria, CategoriaResponse.class))
                .toList();
    }

    public Categoria buscarOuFalhar(Long id) {
        return categoriaDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria " + id + " não encontrada."));
    }

    /**
     * Busca pelo nome uma categoria que a API precisa para funcionar (ex.: "Pagamento de dívidas").
     * Se não existir, o banco não foi preparado com o database/script.sql: isso é erro de
     * instalação (500), não erro do usuário.
     */
    public Categoria obrigatoria(String nome) {
        return categoriaDAO.buscarPorNome(nome).orElseThrow(() -> new IllegalStateException(
                "Categoria padrão '" + nome + "' não existe no banco. Rode o database/script.sql."));
    }

    /** Todas as categorias num mapa nome -> categoria (evita uma consulta por transação gerada). */
    public Map<String, Categoria> mapaPorNome() {
        return categoriaDAO.listar().stream()
                .collect(Collectors.toMap(Categoria::getNome, Function.identity()));
    }
}
