package it.unisa.diem.wordageddon_g16.services.tasks;

import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.WDM;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.Collection;

public class DocAnalyzerServiceFX extends Service<WDM> {
    // Restituisco un WDM, che contiene le informazioni sul documento analizzato
    // o meglio associa il documento ad una lista di parale e le loro frequenze.

    private final Document document;
    private final Collection<String> stopWords;

    /**
     * Costruttore del service.
     *
     * @param document  il documento da analizzare
     * @param stopwords la collezione di parole da escludere dall'analisi
     */
    public DocAnalyzerServiceFX(Document document, Collection<String> stopWords) {
        this.document = document;
        this.stopWords = stopWords;
    }
    @Override
    protected Task<WDM> createTask() {
        return null;
    }
}
