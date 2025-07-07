package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.DocumentDAO;
import it.unisa.diem.wordageddon_g16.db.GameReportDAO;
import it.unisa.diem.wordageddon_g16.db.StopWordDAO;
import it.unisa.diem.wordageddon_g16.db.WdmDAO;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Difficulty;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.WDM;

import java.time.Duration;
import java.util.*;

/**
 * Service principale per la gestione della logica di gioco di Wordageddon.
 * Si occupa della generazione delle domande, della selezione dei documenti,
 * della gestione dei parametri di partita e dell'interazione con il database.
 */
public class GameService {

    private final GameReportDAO gameReportDAO;
    private final WdmDAO wdmDAO;
    private final DocumentDAO documentDAO;
    private final StopWordDAO stopwordDAO;
    private final AppContext context;

    private GameParams params;
    private Map<Document, WDM> wdmMap;

    /**
     * Costruisce un nuovo GameService.
     *
     * @param context       il contesto applicativo corrente
     * @param gameReportDAO DAO per i report di gioco
     * @param wdmDAO        DAO per le matrici parola-documento
     * @param documentDAO   DAO per i documenti
     * @param stopwordDAO   DAO per le stopword
     */
    public GameService(AppContext context, GameReportDAO gameReportDAO, WdmDAO wdmDAO,
                       DocumentDAO documentDAO, StopWordDAO stopwordDAO) {
        this.gameReportDAO = gameReportDAO;
        this.wdmDAO = wdmDAO;
        this.documentDAO = documentDAO;
        this.stopwordDAO = stopwordDAO;
        this.context = context;
    }

    /**
     * Inizializza la partita con la difficoltà specificata.
     *
     * @param difficulty la difficoltà scelta per la partita
     */
    public void init(Difficulty difficulty) {
        params = new GameParams(difficulty);
        wdmMap = new HashMap<>();
    }

    /**
     * Restituisce la difficoltà della partita corrente.
     *
     * @return la difficoltà selezionata
     * @throws IllegalStateException se la partita non è stata inizializzata
     */
    public Difficulty getDifficulty() {
        if (params == null) {
            throw new IllegalStateException("Game not initialized");
        }
        return params.difficulty;
    }

    /**
     * Restituisce il tempo limite della partita corrente.
     *
     * @return la durata massima concessa per la partita
     * @throws IllegalStateException se la partita non è stata inizializzata
     */
    public Duration getTimeLimit() {
        if (params == null) {
            throw new IllegalStateException("Game not initialized");
        }
        return params.timer;
    }

    /**
     * Restituisce la lista dei documenti selezionati per la partita corrente.
     *
     * @return lista dei documenti utilizzati nella partita
     * @throws IllegalStateException se la partita non è stata inizializzata
     */
    public List<Document> getDocuments() {
        if (params == null) {
            throw new IllegalStateException("Game not initialized");
        }
        return params.documents;
    }

    /**
     * Restituisce il numero di domande della partita corrente.
     *
     * @return numero di domande generate per la partita
     * @throws IllegalStateException se la partita non è stata inizializzata
     */
    public int getQuestionCount() {
        if (params == null) {
            throw new IllegalStateException("Game not initialized");
        }
        return params.questionCount;
    }

    /**
     * Genera e restituisce la lista delle domande per la partita corrente.
     * Questo metodo dovrebbe essere chiamato in modo asincrono durante la fase di visualizzazione dei documenti,
     * poiché la generazione delle domande può richiedere tempo in caso di nuovi documenti.
     *
     * @return lista delle domande da sottoporre durante il quiz
     * @throws IllegalStateException se la partita non è stata inizializzata
     */
    public List<Question> getQuestions() {
        if (params == null) throw new IllegalStateException("Game not initialized");
        loadWdmMap();
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < params.questionCount; i++) {
            var type = Question.QuestionType.getRandomType();
            Question q = switch (type) {
                case ABSOLUTE_FREQUENCY -> absoluteFrequencyQuestion();
                case WHICH_MORE -> whichMoreQuestion();
                case WHICH_LESS -> whichLessQuestion();
                case WHICH_DOCUMENT -> whichDocumentQuestion();
                case WHICH_ABSENT -> whichAbsentQuestion();
            };
            questions.add(q);
        }
        return questions;
    }

    /**
     * Carica nella mappa wdmMap le matrici parola-documento per tutti i documenti della partita.
     * Se la matrice non è presente nel database, viene generata e salvata.
     */
    private void loadWdmMap() {
        for (Document doc : params.documents) {
            WDM wdm;
            var optionalWdm = wdmDAO.selectById(doc);
            if (optionalWdm.isEmpty()) {
                wdm = new WDM(doc, stopwordDAO.selectAll());
                wdmDAO.insert(wdm);
            } else {
                wdm = optionalWdm.get();
            }
            wdmMap.put(doc, wdm);
        }
    }

    /**
     * Crea una domanda che chiede quante volte una parola appare in un documento.
     * Seleziona una parola a caso da un documento scelto casualmente e genera risposte multiple.
     *
     * @return una domanda di tipo "frequenza assoluta"
     * @throws IllegalStateException se non sono disponibili parole
     */
    private Question absoluteFrequencyQuestion() {
        List<Document> docs = params.documents;
        Document document = docs.get(GameParams.random.nextInt(docs.size()));
        WDM wdm = wdmMap.get(document);

        List<String> words = new ArrayList<>(wdm.getWords().keySet());
        if (words.isEmpty()) throw new IllegalStateException("No words available");

        String word = words.get(GameParams.random.nextInt(words.size()));
        int frequency = wdm.getWords().get(word);

        Set<Integer> wrongAnswers = new HashSet<>();
        while (wrongAnswers.size() < 3) {
            int answerIndex = frequency + GameParams.random.nextInt(5) - 2;
            if (answerIndex != frequency && answerIndex > 0) {
                wrongAnswers.add(answerIndex);
            }
        }
        List<String> answers = new ArrayList<>();
        for (int answerIndex : wrongAnswers) {
            answers.add(String.valueOf(answerIndex));
        }
        int correctAnswerIndex = GameParams.random.nextInt(4);
        answers.add(correctAnswerIndex, String.valueOf(frequency));
        return Question.create("Quante volte appare la parola " + word + " nel documento " + document.title() + "?", answers, correctAnswerIndex);
    }

    /**
     * Crea una domanda che chiede quale parola appare più frequentemente tra quelle proposte.
     * Seleziona quattro parole da un documento e chiede quale ha la frequenza maggiore.
     *
     * @return una domanda di tipo "quale appare di più"
     */
    private Question whichMoreQuestion() {
        List<Document> docs = params.documents;
        Document document = docs.get(GameParams.random.nextInt(docs.size()));
        WDM wdm = wdmMap.get(document);

        List<Map.Entry<String, Integer>> wordFrequency = new ArrayList<>(wdm.getWords().entrySet());
        Collections.shuffle(wordFrequency);

        List<Map.Entry<String, Integer>> currentAnswer = new ArrayList<>();
        for (int y = 0; y < 4; y++) {
            currentAnswer.add(wordFrequency.get(y));
        }

        List<String> answers = new ArrayList<>();
        int correctIndex = 0;
        int maxFreq = -1;
        for (int i = 0; i < currentAnswer.size(); i++) {
            Map.Entry<String, Integer> entry = currentAnswer.get(i);
            answers.add(entry.getKey());
            if (entry.getValue() > maxFreq) {
                maxFreq = entry.getValue();
                correctIndex = i;
            }
        }
        return Question.create("Quale di queste parole appare più frequentemente nel documento " + document.title() + "?", answers, correctIndex);
    }

    /**
     * Crea una domanda che chiede quale parola appare meno frequentemente tra quelle proposte.
     * Seleziona quattro parole da un documento e chiede quale ha la frequenza minore.
     *
     * @return una domanda di tipo "quale appare di meno"
     */
    private Question whichLessQuestion() {
        List<Document> docs = params.documents;
        Document document = docs.get(GameParams.random.nextInt(docs.size()));
        WDM wdm = wdmMap.get(document);

        List<Map.Entry<String, Integer>> wordFrequency = new ArrayList<>(wdm.getWords().entrySet());
        Collections.shuffle(wordFrequency);

        List<Map.Entry<String, Integer>> selected = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            selected.add(wordFrequency.get(i));
        }

        int correctIndex = 0;
        int minFreq = Integer.MAX_VALUE;
        for (int i = 0; i < selected.size(); i++) {
            if (selected.get(i).getValue() < minFreq) {
                minFreq = selected.get(i).getValue();
                correctIndex = i;
            }
        }
        List<String> answers = new ArrayList<>();
        for (var entry : selected) {
            answers.add(entry.getKey());
        }
        return Question.create("Quale delle seguenti parole appare meno frequentemente nel documento " + document.title() + "?", answers, correctIndex);
    }

    /**
     * Crea una domanda che chiede in quale documento appare una determinata parola.
     * Seleziona una parola da un documento e propone quattro documenti come possibili risposte.
     *
     * @return una domanda di tipo "in quale documento"
     * @throws IllegalStateException se non sono disponibili parole
     */
    private Question whichDocumentQuestion() {
        List<Document> docs = params.documents;
        Document document = docs.get(GameParams.random.nextInt(docs.size()));
        WDM wdm = wdmMap.get(document);

        List<String> words = new ArrayList<>(wdm.getWords().keySet());
        if (words.isEmpty()) throw new IllegalStateException("No words available");

        String word = words.get(GameParams.random.nextInt(words.size()));

        List<Document> docAnswer = new ArrayList<>();
        docAnswer.add(document);
        while (docAnswer.size() < 4) {
            Document d = docs.get(GameParams.random.nextInt(docs.size()));
            docAnswer.add(d);
        }
        Collections.shuffle(docAnswer);

        List<String> answers = new ArrayList<>();
        int index = -1;
        for (int i = 0; i < docAnswer.size(); i++) {
            Document d = docAnswer.get(i);
            answers.add(d.title());
            if (d.equals(document)) {
                index = i;
            }
        }
        return Question.create("In quale di questi documenti appare la parola " + word + "?", answers, index);
    }

    /**
     * Crea una domanda che chiede quale parola NON appare in nessun documento.
     * Seleziona tre parole presenti e ne aggiunge una assente, poi chiede all'utente di individuarla.
     *
     * @return una domanda di tipo "quale assente"
     * @throws IllegalStateException se non ci sono abbastanza parole per generare la domanda
     */
    private Question whichAbsentQuestion() {
        List<Document> docs = params.documents;
        Set<String> allWords = new HashSet<>();
        for (WDM wdm : wdmMap.values()) {
            allWords.addAll(wdm.getWords().keySet());
        }
        if (allWords.size() < 3) {
            throw new IllegalStateException("Not enough words for the question");
        }

        List<String> presentWords = new ArrayList<>(allWords);
        Collections.shuffle(presentWords);
        List<String> answers = new ArrayList<>();
        answers.add(presentWords.get(0));
        answers.add(presentWords.get(1));
        answers.add(presentWords.get(2));

        String absentWord = "rossella"; // Da migliorare: generare parola assente randomica
        answers.add(absentWord);

        Collections.shuffle(answers);
        int correctIndex = answers.indexOf(absentWord);

        return Question.create("Quale delle seguenti parole NON è presente in nessun documento?", answers, correctIndex);
    }

    /**
     * Record che rappresenta una domanda del quiz.
     *
     * @param text               testo della domanda
     * @param answers            elenco delle possibili risposte
     * @param correctAnswerIndex indice della risposta corretta
     */
    public record Question(
            String text,
            List<String> answers,
            int correctAnswerIndex
    ) {
        /**
         * Tipologie di domande disponibili nel quiz.
         */
        enum QuestionType {
            ABSOLUTE_FREQUENCY(1f), // Quante volte appare una parola
            WHICH_MORE(0.5f),       // Quale parola appare più spesso tra quelle proposte
            WHICH_LESS(0.5f),       // Quale parola appare meno spesso tra quelle proposte
            WHICH_DOCUMENT(1f),     // Quale documento contiene una parola
            WHICH_ABSENT(1f);       // Quale parola non è presente in nessun documento

            private final float weight;

            QuestionType(float weight) {
                this.weight = weight;
            }

            /**
             * Restituisce una tipologia di domanda casuale.
             *
             * @return tipo di domanda scelto casualmente
             */
            public static QuestionType getRandomType() {
                var types = values();
                return types[GameParams.random.nextInt(types.length)];
            }
        }

        /**
         * Crea una nuova domanda.
         *
         * @param text               testo della domanda
         * @param answers            elenco delle possibili risposte
         * @param correctAnswerIndex indice della risposta corretta
         * @return una nuova istanza di Question
         * @throws IllegalArgumentException se i parametri non sono validi
         */
        public static Question create(String text, List<String> answers, int correctAnswerIndex) {
            if (text == null || answers == null || correctAnswerIndex < 0 || correctAnswerIndex >= answers.size()) {
                throw new IllegalArgumentException("Invalid question parameters");
            }
            return new Question(text, answers, correctAnswerIndex);
        }
    }

    /**
     * Classe interna che incapsula i parametri di una partita.
     */
    private class GameParams {

        private static final Random random = new Random();

        private final Duration timer;
        private final List<Document> documents;
        private final int questionCount;
        private final Difficulty difficulty;

        /**
         * Classe di supporto per la gestione della difficoltà.
         */
        private static class DifficultyIndex {
            private final float cap;
            private float value;

            /**
             * Costruisce un nuovo DifficultyIndex.
             *
             * @param cap valore massimo della difficoltà
             */
            public DifficultyIndex(float cap) {
                this.cap = cap;
                this.value = cap;
            }

            /**
             * Restituisce un valore casuale e lo sottrae da value.
             *
             * @return valore casuale
             */
            public float getNext() {
                var result = random.nextFloat(value);
                value -= result;
                return result;
            }

            /**
             * Restituisce il prossimo valore relativo rispetto al cap.
             *
             * @return valore relativo
             */
            public float getNextRelative() {
                return getNext() / cap;
            }

            /**
             * Restituisce il valore rimanente.
             *
             * @return valore rimanente
             */
            public float getRemaining() {
                return value;
            }

            /**
             * Restituisce il valore massimo della difficoltà.
             *
             * @return cap
             */
            public float getCap() {
                return cap;
            }
        }

        /**
         * Costruisce i parametri della partita in base alla difficoltà selezionata.
         *
         * @param difficulty la difficoltà scelta per la partita
         */
        private GameParams(Difficulty difficulty) {
            this.difficulty = difficulty;

            var cap = switch (difficulty) {
                case EASY -> 1.0f;
                case MEDIUM -> 2.0f;
                case HARD -> 3.0f;
            };
            var di = new DifficultyIndex(cap);
            documents = Collections.unmodifiableList(generateDocuments(di.getNextRelative()));
            timer = generateTimer(di.getNextRelative());
            questionCount = generateQuestionCount(di.getRemaining());
        }

        /**
         * Seleziona i documenti da utilizzare per la partita in base all'influenza della difficoltà.
         *
         * @param influence valore di influenza della difficoltà
         * @return lista di documenti selezionati
         * @throws IllegalArgumentException se non sono disponibili documenti
         */
        private List<Document> generateDocuments(float influence) throws IllegalArgumentException {
            int maxWords = 1000;
            int minWords = 200;
            int wordCountTolerance = 50;

            var result = new ArrayList<Document>();

            var docList = new ArrayList<>(documentDAO.selectAll());
            if (docList.isEmpty()) {
                throw new IllegalStateException("No documents available for the game");
            }

            int wordsNeeded = Math.round(minWords + (maxWords - minWords) * influence);
            var documentsLeft = docList.size();
            do {
                Document currentDoc = docList.get(random.nextInt(documentsLeft--));
                docList.remove(currentDoc);

                int remainder = wordsNeeded - currentDoc.wordCount();
                if (remainder > wordCountTolerance) {
                    wordsNeeded = remainder;
                    result.add(currentDoc);
                } else if (remainder > -wordCountTolerance) {
                    result.add(currentDoc);
                    break;
                } else if (documentsLeft == 0) {
                    if (-remainder < wordsNeeded)
                        result.add(currentDoc);
                    break;
                }
            } while (documentsLeft > 0);
            return result;
        }

        /**
         * Genera il timer della partita in base all'influenza della difficoltà.
         *
         * @param influence valore di influenza della difficoltà
         * @return durata del timer
         */
        private Duration generateTimer(float influence) {
            int timerMax = 10 * 60; // secondi
            int timerMin = 2 * 60; // secondi

            return Duration.ofSeconds((long) (timerMax - (timerMax - timerMin) * (influence)));
        }

        /**
         * Genera il numero di domande per la partita in base all'influenza della difficoltà.
         *
         * @param influence valore di influenza della difficoltà
         * @return numero di domande
         */
        private int generateQuestionCount(float influence) {
            int maxQuestions = 20;
            int minQuestions = 5;

            return (int) (minQuestions + (maxQuestions - minQuestions) * influence);
        }
    }
}
