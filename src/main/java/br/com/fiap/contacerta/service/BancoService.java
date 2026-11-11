package br.com.fiap.contacerta.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.BancoDAO;
import br.com.fiap.contacerta.dto.conta.BancoResponse;

/** Catálogo de bancos: só leitura (os bancos são inseridos pelo script do banco de dados). */
@Service
public class BancoService {

    private final BancoDAO bancoDAO;
    private final ModelMapper mapper;

    public BancoService(BancoDAO bancoDAO, ModelMapper mapper) {
        this.bancoDAO = bancoDAO;
        this.mapper = mapper;
    }

    public List<BancoResponse> listar() {
        return bancoDAO.listar().stream()
                .map(banco -> mapper.map(banco, BancoResponse.class))
                .toList();
    }
}
