package br.com.fiap.contacerta.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.DividaDAO;
import br.com.fiap.contacerta.dao.PagamentoDAO;
import br.com.fiap.contacerta.dto.divida.DividaRequest;
import br.com.fiap.contacerta.dto.divida.DividaResponse;
import br.com.fiap.contacerta.exception.RecursoNaoEncontradoException;
import br.com.fiap.contacerta.exception.RegraNegocioException;
import br.com.fiap.contacerta.exception.RequisicaoInvalidaException;
import br.com.fiap.contacerta.model.ContaBancaria;
import br.com.fiap.contacerta.model.Divida;
import br.com.fiap.contacerta.model.StatusDivida;

/** Regras do cadastro manual de dívidas. */
@Service
public class DividaService {

    private final DividaDAO dividaDAO;
    private final PagamentoDAO pagamentoDAO;
    private final UsuarioService usuarioService;
    private final ContaService contaService;
    private final ModelMapper mapper;

    public DividaService(DividaDAO dividaDAO, PagamentoDAO pagamentoDAO, UsuarioService usuarioService,
                         ContaService contaService, ModelMapper mapper) {
        this.dividaDAO = dividaDAO;
        this.pagamentoDAO = pagamentoDAO;
        this.usuarioService = usuarioService;
        this.contaService = contaService;
        this.mapper = mapper;
    }

    public List<DividaResponse> listarPorUsuario(Long usuarioId) {
        usuarioService.buscarOuFalhar(usuarioId);
        return dividaDAO.listarPorUsuario(usuarioId).stream().map(this::paraResponse).toList();
    }

    public DividaResponse buscar(Long id) {
        return paraResponse(buscarOuFalhar(id));
    }

    public DividaResponse criar(Long usuarioId, DividaRequest request) {
        usuarioService.buscarOuFalhar(usuarioId);
        validarContaDoUsuario(request.getContaId(), usuarioId);

        // DTO -> model (o texto do tipo vira o enum TipoDivida)
        Divida divida = mapper.map(request, Divida.class);
        divida.setUsuarioId(usuarioId);
        divida.setCredor(request.getCredor().trim());
        divida.setSimulada(false);
        completarCampos(divida, LocalDate.now());

        dividaDAO.inserir(divida);
        return paraResponse(divida);
    }

    public DividaResponse atualizar(Long id, DividaRequest request) {
        Divida existente = buscarOuFalhar(id);
        validarContaDoUsuario(request.getContaId(), existente.getUsuarioId());

        Divida divida = mapper.map(request, Divida.class);
        divida.setId(id);
        divida.setUsuarioId(existente.getUsuarioId());
        divida.setCredor(request.getCredor().trim());
        divida.setSimulada(existente.isSimulada());
        completarCampos(divida, LocalDate.now());

        dividaDAO.atualizar(divida);
        return paraResponse(divida);
    }

    public void remover(Long id) {
        buscarOuFalhar(id);
        if (pagamentoDAO.contarPorDivida(id) > 0) {
            throw new RegraNegocioException(
                    "Não é possível excluir a dívida: ela já possui pagamentos registrados.");
        }
        dividaDAO.remover(id);
    }

    public Divida buscarOuFalhar(Long id) {
        return dividaDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Dívida " + id + " não encontrada."));
    }

    /** A conta é opcional; se vier, precisa existir (404) e ser do mesmo usuário (409). */
    private void validarContaDoUsuario(Long contaId, Long usuarioId) {
        if (contaId == null) {
            return;
        }
        ContaBancaria conta = contaService.buscarOuFalhar(contaId);
        if (!conta.getUsuarioId().equals(usuarioId)) {
            throw new RegraNegocioException("A conta informada pertence a outro usuário.");
        }
    }

    /**
     * Preenche o que o usuário não informou e calcula o status.
     * É estático e sem banco de dados para poder ser testado sozinho (ver DividaServiceTest).
     */
    static void completarCampos(Divida divida, LocalDate hoje) {
        if (divida.getSaldoDevedor() == null) {
            divida.setSaldoDevedor(divida.getValorOriginal());
        }
        if (divida.getParcelasPagas() == null) {
            divida.setParcelasPagas(0);
        }
        if (divida.getParcelasPagas() > divida.getParcelasTotal()) {
            throw new RequisicaoInvalidaException("As parcelas pagas não podem ser mais que o total de parcelas.");
        }
        if (divida.getValorParcela() == null) {
            divida.setValorParcela(CalculadoraFinanceira.parcelaPrice(
                    divida.getValorOriginal(), divida.getTaxaJurosMensal(), divida.getParcelasTotal()));
        }
        if (divida.getDiasAtraso() == null) {
            LocalDate vencimento = divida.getDataProximoVencimento();
            boolean vencida = vencimento != null && vencimento.isBefore(hoje);
            divida.setDiasAtraso(vencida ? (int) ChronoUnit.DAYS.between(vencimento, hoje) : 0);
        }

        // O status é sempre CALCULADO: saldo zerado = quitada; com dias de atraso = atrasada.
        if (divida.getSaldoDevedor().compareTo(BigDecimal.ZERO) == 0) {
            divida.setStatus(StatusDivida.QUITADA);
            divida.setDiasAtraso(0);
        } else if (divida.getDiasAtraso() > 0) {
            divida.setStatus(StatusDivida.ATRASADA);
        } else {
            divida.setStatus(StatusDivida.EM_DIA);
        }
    }

    /** model -> DTO de saída, acrescentando a descrição do tipo e as parcelas restantes. */
    public DividaResponse paraResponse(Divida divida) {
        DividaResponse response = mapper.map(divida, DividaResponse.class);
        response.setTipoDescricao(divida.getTipo().getDescricao());
        response.setParcelasRestantes(divida.getParcelasTotal() - divida.getParcelasPagas());
        return response;
    }
}
