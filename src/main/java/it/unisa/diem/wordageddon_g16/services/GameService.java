package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.DocumentDAO;
import it.unisa.diem.wordageddon_g16.db.GameReportDAO;
import it.unisa.diem.wordageddon_g16.db.StopWordDAO;
import it.unisa.diem.wordageddon_g16.db.WdmDAO;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Difficulty;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.WDM;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servizio principale che gestisce la logica di gioco di Wordageddon.
 * Si occupa di:
 * <ul>
 *   <li>Generazione delle domande del quiz</li>
 *   <li>Selezione e gestione dei documenti di gioco</li>
 *   <li>Gestione dei parametri della partita in base alla difficoltà</li>
 *   <li>Interazione con il database tramite i DAO</li>
 * </ul>
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
     * Costruisce un nuovo GameService, inizializzando i DAO e il contesto applicativo.
     *
     * @param context       il contesto applicativo corrente
     * @param gameReportDAO DAO per la gestione dei report di gioco
     * @param wdmDAO        DAO per la gestione delle matrici parola-documento
     * @param documentDAO   DAO per la gestione dei documenti
     * @param stopwordDAO   DAO per la gestione delle stopword
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
     * Inizializza una nuova partita con la difficoltà specificata.
     * Prepara i parametri di gioco e la mappa delle matrici parola-documento.
     *
     * @param difficulty la difficoltà scelta per la partita
     */
    public void init(Difficulty difficulty) {
        params = new GameParams(difficulty);
        wdmMap = new HashMap<>();
    }

    /**
     * Restituisce la difficoltà attualmente selezionata per la partita.
     *
     * @return la difficoltà della partita
     * @throws IllegalStateException se la partita non è stata inizializzata
     */
    public Difficulty getDifficulty() {
        if (params == null) {
            throw new IllegalStateException("Game not initialized");
        }
        return params.difficulty;
    }
    public int getScorePerQuestion() {
        int totalScore = Difficulty.getMaxScoreDifficulty(getDifficulty());
        return totalScore / getQuestionCount();
    }

    /**
     * Restituisce il tempo limite della partita corrente.
     *
     * @return la durata massima concessa per la partita (oggetto Duration)
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
     * Restituisce il numero di domande generate per la partita corrente.
     *
     * @return numero di domande del quiz
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
     * La generazione può essere lenta se sono presenti nuovi documenti.
     * Da chiamare in modo asincrono durante la fase di visualizzazione dei documenti.
     *
     * @return lista delle domande del quiz
     * @throws IllegalStateException se la partita non è stata inizializzata
     */
    public List<Question> getQuestions() {
        Random rand = new Random();
        if (params == null) throw new IllegalStateException("Game not initialized");
        loadWdmMap();
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < params.questionCount; i++) {
            var type = Question.QuestionType.getRandomType();
            Question q = switch (type) {
                // I metodi single generano una domanda basata su un singolo documento,
                // mentre gli altri metodi generano domande basate su tutti i documenti.
                case ABSOLUTE_FREQUENCY -> rand.nextBoolean()
                        ? absoluteFrequencyQuestionSingle()
                        : absoluteFrequencyQuestion();
                case WHICH_MORE -> rand.nextBoolean() ?
                        whichMoreQuestionSingle()
                        : whichMoreQuestion();
                case WHICH_LESS -> rand.nextBoolean()
                        ? whichLessQuestionSingle()
                        : whichLessQuestion();
                case WHICH_DOCUMENT -> whichDocumentQuestion();
                case WHICH_ABSENT -> whichAbsentQuestion();
            };
            questions.add(q);
        }
        return questions;
    }

    private Question absoluteFrequencyQuestionSingle() {
        // Seleziona un documento casuale
        List<Document> docs = getDocuments();
        Document document = docs.get(GameParams.random.nextInt(docs.size()));
        WDM wdm = wdmMap.get(document);

        // Seleziona una parola casuale tra quelle presenti nel documento
        List<String> words = new ArrayList<>(wdm.getWords().keySet());
        String chosenWord = words.get(GameParams.random.nextInt(words.size()));
        int correctFrequency = wdm.getWords().get(chosenWord);

        // Genera risposte plausibili (inclusa quella corretta)
        Set<Integer> options = new HashSet<>();
        options.add(correctFrequency);
        Random rand = new Random();
        while (options.size() < 4) {
            int delta = 1 + rand.nextInt(Math.max(1, correctFrequency / 2 + 2));
            int fakeOption = rand.nextBoolean() ? correctFrequency + delta : Math.max(0, correctFrequency - delta);
            options.add(fakeOption);
        }

        // Prepara la lista delle risposte e trova l'indice corretto
        List<Integer> answerOptions = new ArrayList<>(options);
        Collections.shuffle(answerOptions);
        int correctIndex = answerOptions.indexOf(correctFrequency);

        // Converte le risposte in stringhe
        List<String> answers = answerOptions.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());

        // Crea la domanda
        return Question.create(
                "Quante volte la parola \"" + chosenWord.toUpperCase() + "\" appare nel documento \"" + document.title() + "\"?",
                answers,
                correctIndex
        );
    }

    private Question absoluteFrequencyQuestion() {
        // Crea la mappa cumulativa delle frequenze per tutte le parole in tutti i documenti
        Map<String, Integer> cumulativeFrequency = new HashMap<>();
        for (WDM wdm : wdmMap.values()) {
            for (Map.Entry<String, Integer> entry : wdm.getWords().entrySet()) {
                cumulativeFrequency.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        // Se non ci sono parole, lancia eccezione
        if (cumulativeFrequency.isEmpty()) {
            throw new IllegalStateException("Non ci sono parole nei documenti");
        }

        // Seleziona una parola casuale tra quelle presenti
        List<String> words = new ArrayList<>(cumulativeFrequency.keySet());
        String chosenWord = words.get(GameParams.random.nextInt(words.size()));
        int correctFrequency = cumulativeFrequency.get(chosenWord);

        // Genera risposte plausibili (inclusa quella corretta)
        Set<Integer> options = new HashSet<>();
        options.add(correctFrequency);
        Random rand = new Random();
        while (options.size() < 4) {
            // Genera un'opzione casuale vicina al valore corretto
            int delta = 1 + rand.nextInt(Math.max(1, correctFrequency / 2 + 2));
            int fakeOption = rand.nextBoolean() ? correctFrequency + delta : Math.max(0, correctFrequency - delta);
            options.add(fakeOption);
        }

        // Prepara la lista delle risposte e trova l'indice corretto
        List<Integer> answerOptions = new ArrayList<>(options);
        Collections.shuffle(answerOptions);
        int correctIndex = answerOptions.indexOf(correctFrequency);

        // Converte le risposte in stringhe
        List<String> answers = answerOptions.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());

        // Crea la domanda
        return Question.create(
                "Quante volte la parola \"" + chosenWord.toUpperCase() + "\" appare in tutti i documenti?",
                answers,
                correctIndex
        );
    }

    private Question whichMoreQuestion() {
        // Mappa cumulativa delle frequenze di tutte le parole in tutti i documenti
        Map<String, Integer> cumulativeFrequency = new HashMap<>();
        List<Document> docs = getDocuments();
        for (Document doc : docs) {
            WDM wdm = wdmMap.get(doc);
            if (wdm != null) {
                for (Map.Entry<String, Integer> entry : wdm.getWords().entrySet()) {
                    // accumulo valori associati ad una determinata chiave
                    cumulativeFrequency.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }

        // Lista delle entry parola-frequenza, mischiate per selezione casuale
        List<Map.Entry<String, Integer>> wordFrequency = new ArrayList<>(cumulativeFrequency.entrySet());
        Collections.shuffle(wordFrequency);

        // Prendi le prime 4 parole casuali
        List<Map.Entry<String, Integer>> currentAnswer = new ArrayList<>();
        for (int y = 0; y < 4 && y < wordFrequency.size(); y++) {
            currentAnswer.add(wordFrequency.get(y));
        }

        // Trova la parola più frequente tra le 4 selezionate
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

        return Question.create("Quale di queste parole appare più frequentemente in tutti i documenti?", answers, correctIndex);
    }

    private Question whichMoreQuestionSingle() {
        List<Document> docs = getDocuments();
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

    private Question whichLessQuestionSingle() {
        List<Document> docs = getDocuments();
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

    private Question whichLessQuestion() {
        // Mappa cumulativa delle frequenze di tutte le parole in tutti i documenti
        Map<String, Integer> cumulativeFrequency = new HashMap<>();
        List<Document> docs = getDocuments();
        for (Document doc : docs) {
            WDM wdm = wdmMap.get(doc);
            if (wdm != null) {
                for (Map.Entry<String, Integer> entry : wdm.getWords().entrySet()) {
                    // Accumulo valori associati ad una determinata chiave
                    cumulativeFrequency.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }

        // Lista delle entry parola-frequenza, mischiate per selezione casuale
        List<Map.Entry<String, Integer>> wordFrequency = new ArrayList<>(cumulativeFrequency.entrySet());
        Collections.shuffle(wordFrequency);

        // Prendi le prime 4 parole casuali
        List<Map.Entry<String, Integer>> currentAnswer = new ArrayList<>();
        for (int y = 0; y < 4 && y < wordFrequency.size(); y++) {
            currentAnswer.add(wordFrequency.get(y));
        }

        // Trova la parola MENO frequente tra le 4 selezionate
        List<String> answers = new ArrayList<>();
        int correctIndex = 0;
        int minFreq = Integer.MAX_VALUE;
        for (int i = 0; i < currentAnswer.size(); i++) {
            Map.Entry<String, Integer> entry = currentAnswer.get(i);
            answers.add(entry.getKey());
            if (entry.getValue() < minFreq) {
                minFreq = entry.getValue();
                correctIndex = i;
            }
        }

        return Question.create("Quale di queste parole appare meno frequentemente in tutti i documenti?", answers, correctIndex);
    }

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
        return Question.create("In quale di questi documenti appare la parola " + word.toUpperCase() + "?", answers, index);
    }

    private Question whichAbsentQuestion() {
        // Recupera tutti i documenti e tutte le parole presenti
        List<Document> docs = params.documents;
        Set<String> allWords = new HashSet<>();
        for (WDM wdm : wdmMap.values()) {
            allWords.addAll(wdm.getWords().keySet());
        }
        if (allWords.size() < 3) {
            throw new IllegalStateException("Not enough words for the question");
        }

        // Crea una lista di parole presenti e seleziona 3 parole casuali
        List<String> presentWords = new ArrayList<>(allWords);
        Collections.shuffle(presentWords);
        List<String> answers = new ArrayList<>();
        answers.add(presentWords.get(0));
        answers.add(presentWords.get(1));
        answers.add(presentWords.get(2));

        // Genera una parola assente in modo robusto
        String absentWord = generateAbsentWord(allWords);

        answers.add(absentWord);

        // Mischia le risposte e individua l'indice corretto
        Collections.shuffle(answers);
        int correctIndex = answers.indexOf(absentWord);

        return Question.create(
                "Quale delle seguenti parole NON è presente in nessun documento?",
                answers,
                correctIndex
        );
    }

    /**
     * DA RIFARE
     * Genera una parola che sicuramente non è presente nel set delle parole.
     * Puoi personalizzare la logica per generare parole più realistiche.
     *
     * @param presentWords insieme delle parole già presenti nei documenti
     * @return una parola sicuramente assente
     */
    private String generateAbsentWord(Set<String> presentWords) {
        Random rand = new Random();
        String[] candidateSyllables = {"tra", "spo", "gle", "fro", "zan", "qui", "lop"};
        String absentWord;
        do {
            // Costruisce una parola casuale di 2-3 sillabe
            int syllableCount = 2 + rand.nextInt(2); // 2 o 3 sillabe
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < syllableCount; i++) {
                sb.append(candidateSyllables[rand.nextInt(candidateSyllables.length)]);
            }
            absentWord = sb.toString();
        } while (presentWords.contains(absentWord));
        return absentWord;
    }

    /**
     * Carica nella mappa interna le matrici parola-documento (WDM) per tutti i documenti della partita.
     * Se la matrice non esiste nel database, viene generata e salvata automaticamente.
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
         * Enumerazione delle tipologie di domande disponibili nel quiz.
         */
        public enum QuestionType {
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
         * Crea una nuova domanda del quiz, validando i parametri.
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
     * Classe interna che incapsula e gestisce i parametri di una partita,
     * come timer, documenti selezionati, numero di domande e difficoltà.
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
         * @param influence valore di influenza della difficoltà (0-1)
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
         * @return durata del timer come oggetto Duration
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
         * @return numero di domande generate
         */
        private int generateQuestionCount(float influence) {
            int maxQuestions = 20;
            int minQuestions = 5;

            return (int) (minQuestions + (maxQuestions - minQuestions) * influence);
        }
    }

    /**
     * Esegue il parsing dei documenti selezionati e restituisce il testo concatenato.
     * Da utilizzare nella fase di lettura dei documenti prima del quiz.
     *
     * @return testo concatenato dei documenti selezionati
     */
    public StringBuffer setupReadingPhase() {
        StringBuffer text = new StringBuffer();
        for (Document doc : getDocuments()) {
            Path path = Path.of(Config.get(Config.Props.DOCUMENTS_DIR) + doc.filename());
            try {
                text.append(Files.readString(path)).append("\n");
                System.out.println(text);
            } catch (IOException e) {
                SystemLogger.log("Errore nella lettura del documento", e);
            }
        }
        return text;
    }
}
