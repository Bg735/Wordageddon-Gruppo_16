package it.unisa.diem.wordageddon_g16.services.tasks;

import it.unisa.diem.wordageddon_g16.db.StopWordDAO;
import javafx.concurrent.Task;

import java.util.HashSet;
import java.util.Set;

public class StopwordAnalysisTask extends Task<Set<String>> {
    private final String tfRawText;
    private final StopWordDAO stopWordDAO;

    public StopwordAnalysisTask(String tfRawText, StopWordDAO stopWordDAO) {
        this.tfRawText = tfRawText;
        this.stopWordDAO = stopWordDAO;
    }

    @Override
    protected Set<String> call() {


        return null;
    }

}
