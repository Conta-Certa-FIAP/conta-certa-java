package br.com.fiap.contacerta.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.ExecutorTransacao;
import br.com.fiap.contacerta.dao.LicaoConcluidaDAO;
import br.com.fiap.contacerta.dao.PlanoDAO;
import br.com.fiap.contacerta.dao.UsuarioDAO;
import br.com.fiap.contacerta.dto.usuario.LoginRequest;
import br.com.fiap.contacerta.dto.usuario.LoginResponse;
import br.com.fiap.contacerta.dto.usuario.UsuarioAtualizacaoRequest;
import br.com.fiap.contacerta.dto.usuario.UsuarioCadastroRequest;
import br.com.fiap.contacerta.dto.usuario.UsuarioResponse;
import br.com.fiap.contacerta.exception.CredenciaisInvalidasException;
import br.com.fiap.contacerta.exception.RecursoNaoEncontradoException;
import br.com.fiap.contacerta.exception.RegraNegocioException;
import br.com.fiap.contacerta.model.Usuario;
import br.com.fiap.contacerta.seguranca.TokenService;

/** Regras da conta do usuário: cadastro, login e manutenção dos dados. */
@Service
public class UsuarioService {

    private static final int HORAS_TRABALHO_PADRAO = 220;

    private final UsuarioDAO usuarioDAO;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper mapper;
    private final TokenService tokenService;
    private final PlanoDAO planoDAO;
    private final LicaoConcluidaDAO licaoDAO;
    private final ExecutorTransacao executor;

    public UsuarioService(UsuarioDAO usuarioDAO, PasswordEncoder passwordEncoder, ModelMapper mapper,
                          TokenService tokenService, PlanoDAO planoDAO, LicaoConcluidaDAO licaoDAO,
                          ExecutorTransacao executor) {
        this.executor = executor;
        this.usuarioDAO = usuarioDAO;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.tokenService = tokenService;
        this.planoDAO = planoDAO;
        this.licaoDAO = licaoDAO;
    }

    public UsuarioResponse cadastrar(UsuarioCadastroRequest request) {
        // DTO -> model (copia nome, email, cpf, rendaDeclarada e horasTrabalhoMes)
        Usuario usuario = mapper.map(request, Usuario.class);
        usuario.setNome(request.getNome().trim());
        usuario.setEmail(normalizarEmail(request.getEmail()));
        usuario.setCpf(CpfUtil.apenasNumeros(request.getCpf()));

        if (usuarioDAO.buscarPorEmail(usuario.getEmail()).isPresent()) {
            throw new RegraNegocioException("Já existe uma conta com este e-mail.");
        }
        if (usuarioDAO.buscarPorCpf(usuario.getCpf()).isPresent()) {
            throw new RegraNegocioException("Já existe uma conta com este CPF.");
        }

        // A senha digitada nunca é gravada: só o hash BCrypt.
        usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));
        if (usuario.getHorasTrabalhoMes() == null) {
            usuario.setHorasTrabalhoMes(HORAS_TRABALHO_PADRAO);
        }
        usuario.setDataCadastro(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        usuarioDAO.inserir(usuario);
        return paraResponse(usuario);
    }

    public LoginResponse login(LoginRequest request) {
        String login = request.getLogin().trim();

        // Tem "@"? É e-mail. Senão tratamos como CPF (com ou sem máscara).
        Optional<Usuario> encontrado = login.contains("@")
                ? usuarioDAO.buscarPorEmail(normalizarEmail(login))
                : usuarioDAO.buscarPorCpf(CpfUtil.apenasNumeros(login));

        // Mesma exceção para "usuário não existe" e "senha errada": mensagem genérica (401).
        Usuario usuario = encontrado.orElseThrow(CredenciaisInvalidasException::new);
        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }

        LoginResponse response = mapper.map(usuario, LoginResponse.class);
        response.setCpfMascarado(CpfUtil.mascarar(usuario.getCpf()));
        // O token é o "crachá" que o front apresenta nas próximas requisições.
        response.setToken(tokenService.gerar(usuario.getId()));
        response.setUserName(usuario.getNome());
        return response;
    }

    public UsuarioResponse buscar(Long id) {
        return paraResponse(buscarOuFalhar(id));
    }

    public UsuarioResponse atualizar(Long id, UsuarioAtualizacaoRequest request) {
        Usuario usuario = buscarOuFalhar(id);
        String novoEmail = normalizarEmail(request.getEmail());

        // O e-mail novo não pode ser de OUTRO usuário (o dele mesmo pode).
        Optional<Usuario> donoDoEmail = usuarioDAO.buscarPorEmail(novoEmail);
        if (donoDoEmail.isPresent() && !donoDoEmail.get().getId().equals(id)) {
            throw new RegraNegocioException("Já existe uma conta com este e-mail.");
        }

        usuario.setNome(request.getNome().trim());
        usuario.setEmail(novoEmail);
        usuario.setRendaDeclarada(request.getRendaDeclarada());
        if (request.getHorasTrabalhoMes() != null) {
            usuario.setHorasTrabalhoMes(request.getHorasTrabalhoMes());
        }
        usuarioDAO.atualizar(usuario);
        return paraResponse(usuario);
    }

    public void remover(Long id) {
        buscarOuFalhar(id);
        if (usuarioDAO.contarVinculos(id) > 0) {
            throw new RegraNegocioException(
                    "Não é possível excluir o usuário: ele ainda possui contas, dívidas ou cofres. Remova-os antes.");
        }
        // Plano salvo e progresso das trilhas são só preferências: saem junto com o usuário,
        // na mesma transação (se apagar o usuário falhar, eles voltam).
        executor.executar(con -> {
            planoDAO.removerPorUsuario(con, id);
            licaoDAO.removerPorUsuario(con, id);
            usuarioDAO.remover(con, id);
            return null;
        });
    }

    /** Usado também pelos outros services para garantir que o usuário da rota existe (404). */
    public Usuario buscarOuFalhar(Long id) {
        return usuarioDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + id + " não encontrado."));
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }

    /** model -> DTO de saída, acrescentando o CPF mascarado. */
    private UsuarioResponse paraResponse(Usuario usuario) {
        UsuarioResponse response = mapper.map(usuario, UsuarioResponse.class);
        response.setCpfMascarado(CpfUtil.mascarar(usuario.getCpf()));
        return response;
    }
}
