/* package it.unisa.diem.wordageddon_g16.services.tasks;

import it.unisa.diem.wordageddon_g16.db.DocumentDAO;
import it.unisa.diem.wordageddon_g16.db.StopWordDAO;
import it.unisa.diem.wordageddon_g16.db.WdmDAO;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.WDM;
import it.unisa.diem.wordageddon_g16.services.GameService;
import it.unisa.diem.wordageddon_g16.services.GameService.Question;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import javafx.concurrent.Task;
import java.util.*;


public class QuestionGenerationTask extends Task<List<GameService.Question>> {
    private final List<Document> documents;
    private final WdmDAO wdmDAO;
    private final StopWordDAO stopWordDAO;

    public QuestionGenerationTask(List<Document> documents, WdmDAO wdmDAO, StopWordDAO stopWordDAO) {
        this.documents = documents;
        this.wdmDAO = wdmDAO;
        this.stopWordDAO = stopWordDAO;
    }

    @Override
    protected List<Question> call() throws Exception {
        try{
            Map<Document, WDM> wdmMap = new java.util.HashMap<>();
            Collection<String> stopWords = stopWordDAO.selectAll();

            for (Document doc : documents) {
                WDM wdm = wdmDAO.selectById(doc).orElseGet(() -> {
                    WDM newWdm = new WDM(doc, stopWords);
                    wdmDAO.insert(newWdm);
                    return newWdm;
                });
                wdmMap.put(doc, wdm);
            }

            List<Question> questions = new java.util.ArrayList<>();
            java.util.Random random = new java.util.Random();

            //Numero di domande da generare
            int questionCount = 4;

            for (int i = 0; i < questionCount; i++) {
                //scelgo un tipo di domanda casuale
                Question.QuestionType[] types = Question.QuestionType.values();
                Question.QuestionType type = types[random.nextInt(types.length)];

                Question q = null;

                switch (type) {
                    case ABSOLUTE_FREQUENCY -> {
                        //quante volte appare una parola in un documento
                        Document doc = documents.get(random.nextInt(documents.size()));
                        WDM wdm = wdmMap.get(doc);
                        var words = new java.util.ArrayList<>(wdm.getWords().keySet());
                        if (words.isEmpty()) continue; // salta se nessuna parola
                        String word = words.get(random.nextInt(words.size()));
                        int freq = wdm.getWords().get(word);

                        java.util.Set<Integer> wrongAnswers = new java.util.HashSet<>();
                        while (wrongAnswers.size() < 3) {
                            int candidate = freq + random.nextInt(5) - 2;
                            if (candidate != freq && candidate > 0) wrongAnswers.add(candidate);
                        }
                        java.util.List<String> answers = new java.util.ArrayList<>();
                        for (int wa : wrongAnswers) answers.add(String.valueOf(wa));
                        int correctIndex = random.nextInt(4);
                        answers.add(correctIndex, String.valueOf(freq));

                        q = Question.create(
                                "Quante volte appare la parola " + word + " nel documento " + doc.title() + "?",
                                answers,
                                correctIndex
                        );
                    }
                    case WHICH_MORE -> {
                        //quale parola appare più frequentemente in un documento
                        Document doc = documents.get(random.nextInt(documents.size()));
                        WDM wdm = wdmMap.get(doc);
                        var entries = new java.util.ArrayList<>(wdm.getWords().entrySet());
                        if (entries.size() < 4) continue;
                        java.util.Collections.shuffle(entries);
                        var subset = entries.subList(0, 4);

                        int maxFreq = -1;
                        int correctIndex = 0;
                        java.util.List<String> answers = new java.util.ArrayList<>();
                        int idx = 0;
                        for (var e : subset) {
                            answers.add(e.getKey());
                            if (e.getValue() > maxFreq) {
                                maxFreq = e.getValue();
                                correctIndex = idx;
                            }
                            idx++;
                        }

                        q = Question.create(
                                "Quale di queste parole appare più frequentemente nel documento " + doc.title() + "?",
                                answers,
                                correctIndex
                        );
                    }
                    case WHICH_LESS -> {
                        //quale parola appare meno frequentemente in un documento
                        Document doc = documents.get(random.nextInt(documents.size()));
                        WDM wdm = wdmMap.get(doc);
                        var entries = new java.util.ArrayList<>(wdm.getWords().entrySet());
                        if (entries.size() < 4) continue;
                        java.util.Collections.shuffle(entries);
                        var subset = entries.subList(0, 4);

                        int minFreq = Integer.MAX_VALUE;
                        int correctIndex = 0;
                        java.util.List<String> answers = new java.util.ArrayList<>();
                        int idx = 0;
                        for (var e : subset) {
                            answers.add(e.getKey());
                            if (e.getValue() < minFreq) {
                                minFreq = e.getValue();
                                correctIndex = idx;
                            }
                            idx++;
                        }

                        q = Question.create(
                                "Quale delle seguenti parole appare meno frequentemente nel documento " + doc.title() + "?",
                                answers,
                                correctIndex
                        );
                    }
                    case WHICH_DOCUMENT -> {
                        //in quale documento appare una parola
                        Document doc = documents.get(random.nextInt(documents.size()));
                        WDM wdm = wdmMap.get(doc);
                        var words = new java.util.ArrayList<>(wdm.getWords().keySet());
                        if (words.isEmpty()) continue;
                        String word = words.get(random.nextInt(words.size()));

                        java.util.List<Document> options = new java.util.ArrayList<>();
                        options.add(doc);
                        while (options.size() < 4) {
                            Document candidate = documents.get(random.nextInt(documents.size()));
                            if (!options.contains(candidate)) options.add(candidate);
                        }
                        java.util.Collections.shuffle(options);

                        int correctIndex = options.indexOf(doc);
                        java.util.List<String> answers = new java.util.ArrayList<>();
                        for (Document d : options) {
                            answers.add(d.title());
                        }

                        q = Question.create(
                                "In quale di questi documenti appare la parola " + word + "?",
                                answers,
                                correctIndex
                        );
                    }
                    case WHICH_ABSENT -> {
                        //quale parola NON appare in nessun documento
                        java.util.Set<String> allWords = new java.util.HashSet<>();
                        for (WDM wdm : wdmMap.values()) {
                            allWords.addAll(wdm.getWords().keySet());
                        }
                        if (allWords.size() < 3) continue;
                        java.util.List<String> presentWords = new java.util.ArrayList<>(allWords);
                        java.util.Collections.shuffle(presentWords);

                        java.util.List<String> answers = new java.util.ArrayList<>();
                        answers.add(presentWords.get(0));
                        answers.add(presentWords.get(1));
                        answers.add(presentWords.get(2));

                        // parola assente fittizia
                        String absentWord = "parolassente";

                        answers.add(absentWord);
                        java.util.Collections.shuffle(answers);
                        int correctIndex = answers.indexOf(absentWord);

                        q = Question.create(
                                "Quale delle seguenti parole NON è presente in nessun documento?",
                                answers,
                                correctIndex
                        );
                    }
                }

                if (q != null) questions.add(q);
            }

            return questions;
    }catch(Exception e){
            SystemLogger.log("Errore nella generazione delle domande:", e);
            System.out.println("Errore nella generazione delle domande:" + e.getMessage());
            throw new RuntimeException("Errore nella generazione delle domande:", e);}
    }
} */