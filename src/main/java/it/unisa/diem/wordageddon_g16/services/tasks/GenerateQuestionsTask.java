package it.unisa.diem.wordageddon_g16.services.tasks;

import it.unisa.diem.wordageddon_g16.services.GameService;
import it.unisa.diem.wordageddon_g16.services.GameService.Question;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import javafx.concurrent.Task;

import java.util.List;

/**
 * Task JavaFX per generare le domande in background senza bloccare l'interfaccia.
 */
public class GenerateQuestionsTask extends Task<List<Question>> {
    private final GameService gameService;

    /**
     * Costruttore del task.
     * @param gameService servizio di gioco da cui ottenere le domande
     */
    public GenerateQuestionsTask(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    protected List<Question> call() {
        try {
            return gameService.getQuestions();
        } catch (Exception e) {
            SystemLogger.log("Errore nella generazione delle domande", e);
            return List.of();
        }
    }
}