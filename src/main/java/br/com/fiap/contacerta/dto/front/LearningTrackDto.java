package br.com.fiap.contacerta.dto.front;

import java.util.List;

/**
 * Uma trilha de aprendizado com o progresso da pessoa logada (item de GET /learning/tracks).
 *
 * @param lessons na ordem em que devem ser feitas
 * @param tip     texto da caixa "Dica" (pode ser nulo)
 */
public record LearningTrackDto(Long id, String title, int minutesPerLesson, List<Lesson> lessons, String tip) {

    public record Lesson(Long id, String title, String summary, boolean completed) {
    }
}
