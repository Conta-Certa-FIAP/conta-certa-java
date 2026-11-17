package br.com.fiap.contacerta.service.front;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import br.com.fiap.contacerta.dao.LicaoConcluidaDAO;
import br.com.fiap.contacerta.dto.front.DashboardDto;
import br.com.fiap.contacerta.dto.front.LearningTrackDto;
import br.com.fiap.contacerta.dto.front.LearningTrackDto.Lesson;
import br.com.fiap.contacerta.exception.RecursoNaoEncontradoException;
import br.com.fiap.contacerta.service.UsuarioService;
import br.com.fiap.contacerta.service.front.CatalogoTrilhas.Trilha;

/** Trilhas de aprendizado com o progresso da pessoa logada (rotas /learning/tracks). */
@Service
public class TrilhaFrontService {

    private final LicaoConcluidaDAO licaoDAO;
    private final UsuarioService usuarioService;

    public TrilhaFrontService(LicaoConcluidaDAO licaoDAO, UsuarioService usuarioService) {
        this.licaoDAO = licaoDAO;
        this.usuarioService = usuarioService;
    }

    /** GET /learning/tracks */
    public List<LearningTrackDto> listar(Long usuarioId) {
        Set<Long> concluidas = licaoDAO.listarPorUsuario(usuarioId);
        return CatalogoTrilhas.TRILHAS.stream().map(trilha -> paraDto(trilha, concluidas)).toList();
    }

    /** POST /learning/tracks/{trackId}/lessons/{lessonId}/complete: devolve a trilha atualizada. */
    public LearningTrackDto concluir(Long usuarioId, Long trilhaId, Long licaoId) {
        Trilha trilha = CatalogoTrilhas.buscar(trilhaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Trilha " + trilhaId + " não encontrada."));
        boolean licaoDaTrilha = trilha.licoes().stream().anyMatch(licao -> licao.id().equals(licaoId));
        if (!licaoDaTrilha) {
            throw new RecursoNaoEncontradoException("Lição " + licaoId + " não encontrada nesta trilha.");
        }
        usuarioService.buscarOuFalhar(usuarioId);

        licaoDAO.marcar(usuarioId, licaoId);
        return paraDto(trilha, licaoDAO.listarPorUsuario(usuarioId));
    }

    /** Cartão "Aprender" da tela inicial: a primeira trilha ainda não terminada (ou a última, se terminou todas). */
    public DashboardDto.Learning resumo(Long usuarioId) {
        List<LearningTrackDto> trilhas = listar(usuarioId);
        if (trilhas.isEmpty()) {
            return null;
        }
        LearningTrackDto escolhida = trilhas.stream()
                .filter(trilha -> concluidas(trilha) < trilha.lessons().size())
                .findFirst()
                .orElse(trilhas.get(trilhas.size() - 1));
        return new DashboardDto.Learning(escolhida.title(), concluidas(escolhida), escolhida.lessons().size());
    }

    private int concluidas(LearningTrackDto trilha) {
        return (int) trilha.lessons().stream().filter(Lesson::completed).count();
    }

    private LearningTrackDto paraDto(Trilha trilha, Set<Long> concluidas) {
        List<Lesson> licoes = trilha.licoes().stream()
                .map(licao -> new Lesson(licao.id(), licao.titulo(), licao.resumo(), concluidas.contains(licao.id())))
                .toList();
        return new LearningTrackDto(trilha.id(), trilha.titulo(), trilha.minutosPorLicao(), licoes, trilha.dica());
    }
}
