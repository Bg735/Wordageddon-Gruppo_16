package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.JDBCWdmDAO;
import it.unisa.diem.wordageddon_g16.db.contracts.GameReportDAO;
import it.unisa.diem.wordageddon_g16.db.contracts.DocumentDAO;
import it.unisa.diem.wordageddon_g16.db.contracts.StopWordDAO;
import it.unisa.diem.wordageddon_g16.models.*;
import it.unisa.diem.wordageddon_g16.utility.Resources;
import it.unisa.diem.wordageddon_g16.utility.SystemLogger;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service principale per la gestione della logica di gioco di Wordageddon.
 * Si occupa della generazione delle domande, della selezione dei documenti,
 * della gestione dei parametri di partita e dell'interazione con il database.
 */
public class GameService {

    private final GameReportDAO gameReportDAO;
    private final JDBCWdmDAO wdmDAO;
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
    public GameService(AppContext context, GameReportDAO gameReportDAO, JDBCWdmDAO wdmDAO,
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
        int numDocs = params.documents.size();

        // Definisci i tipi di domanda consentiti in base al numero di documenti
        List<Question.QuestionType> allowedTypes = new ArrayList<>();
        allowedTypes.add(Question.QuestionType.ABSOLUTE_FREQUENCY);
        allowedTypes.add(Question.QuestionType.WHICH_MORE);
        allowedTypes.add(Question.QuestionType.WHICH_LESS);

        // WHICH_DOCUMENT e WHICH_ABSENT solo se almeno 4 documenti
        if (numDocs >= 4) {
            allowedTypes.add(Question.QuestionType.WHICH_DOCUMENT);
            allowedTypes.add(Question.QuestionType.WHICH_ABSENT);
        } else {
            System.out.println("Sono presenti meno di 4 documenti, le domande generate saranno di tipo SINGLE (riguardano un singolo documento)");
        }

        for (int i = 0; i < params.questionCount; i++) {
            var type = allowedTypes.get(rand.nextInt(allowedTypes.size()));
            Question q;

            // Se ci sono meno di 4 documenti, solo domande SINGLE
            if (numDocs < 4) {
                q = switch (type) {
                    case ABSOLUTE_FREQUENCY -> absoluteFrequencyQuestionSingle();
                    case WHICH_MORE -> whichMoreQuestionSingle();
                    case WHICH_LESS -> whichLessQuestionSingle();
                    default -> throw new IllegalStateException("Tipo di domanda non supportato con meno di 4 documenti: " + type);
                };
            } else {
                // Se >= 4 documenti, scegli casualmente tra single e non single
                q = switch (type) {
                    case ABSOLUTE_FREQUENCY -> rand.nextBoolean()
                            ? absoluteFrequencyQuestionSingle()
                            : absoluteFrequencyQuestion();
                    case WHICH_MORE -> rand.nextBoolean()
                            ? whichMoreQuestionSingle()
                            : whichMoreQuestion();
                    case WHICH_LESS -> rand.nextBoolean()
                            ? whichLessQuestionSingle()
                            : whichLessQuestion();
                    case WHICH_DOCUMENT -> whichDocumentQuestion();
                    case WHICH_ABSENT -> whichAbsentQuestion();
                };
            }
            questions.add(q);
        }
        return questions;
    }
    private Question absoluteFrequencyQuestionSingle() {
        // Seleziona un documento casuale
        List<Document> docs = getDocuments();
        Document document = docs.get(GameParams.random.nextInt(docs.size()));
        WDM wdm = wdmMap.get(document);

        List<String> words = new ArrayList<>(wdm.getWords().keySet());

        // Seleziona una parola casuale tra quelle presenti nel documento a partire dalla sua matrice WDM
        String chosenWord = words.get(GameParams.random.nextInt(words.size()));
        // frequenza della parola nella WDM
        int correctFrequency = wdm.getWords().get(chosenWord);

        // Genero 4 risposte plausibili (inclusa quella corretta) e le inserisco nel set
        Set<Integer> options = new HashSet<>();
        options.add(correctFrequency);

        Random rand = new Random();
        while (options.size() < 4) {

            int delta = 1 + rand.nextInt(4); // Delta tra 1 e 4
            int fakeOption = correctFrequency + (rand.nextBoolean() ? delta : -delta);

            if (fakeOption >= 0 && fakeOption != correctFrequency) {
                options.add(fakeOption);
            }
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
        // Sostanzialmente é una lista contenente le righe della matrice parola-documento
        List<Map.Entry<String, Integer>> wordFrequency = new ArrayList<>(cumulativeFrequency.entrySet());
        Collections.shuffle(wordFrequency);

        // Check: almeno 4 parole disponibili
        if (wordFrequency.size() < 4) {
            throw new IllegalStateException("Non ci sono abbastanza parole per generare la domanda (minimo 4 richieste)");
        }

        // Prendo le prime 4 parole casuali
        List<Map.Entry<String, Integer>> currentAnswer = new ArrayList<>();
        for (int y = 0; y < 4; y++) {
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
        List<Document> docs = getDocuments();
        Document document = docs.get(GameParams.random.nextInt(docs.size()));
        WDM wdm = wdmMap.get(document);

        List<String> words = new ArrayList<>(wdm.getWords().keySet());
        if (words.isEmpty()) throw new IllegalStateException("No words available");

        String word = words.get(GameParams.random.nextInt(words.size()));

        // Crea una lista di documenti senza duplicati
        List<Document> docPool = new ArrayList<>(docs);
        Collections.shuffle(docPool);

        // Assicurati che il documento corretto sia incluso
        if (!docPool.contains(document)) {
            docPool.set(0, document); // Forza la presenza del documento corretto
        }

        // Prendi i primi 4 documenti (tutti diversi)
        List<Document> docAnswer = new ArrayList<>();
        docAnswer.add(document);
        for (Document d : docPool) {
            if (!d.equals(document) && docAnswer.size() < 4) {
                docAnswer.add(d);
            }
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
        return Question.create(
                "In quale di questi documenti appare la parola " + word.toUpperCase() + "?",
                answers,
                index
        );
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
     * Carica nella mappa wdmMap le matrici parola-documento per tutti i documenti della partita.
     * Se la matrice non è presente nel database, viene generata e salvata.
     */
    private void loadWdmMap() {
        for (Document doc : params.documents) {
            WDM wdm;
            var optionalWdm = wdmDAO.selectById(doc);
            // Se la matrice non esiste nel database, la creo e la salvo al volo
            if (optionalWdm.isEmpty()) {
                throw new IllegalStateException("WDM not found for document: " + doc.title());
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
         * Tipologie di domande disponibili nel quiz.
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

    /**
     * @brief Effettua il parsing dei documenti e prepara il testo per la fase di lettura.
     * Restituisce un StringBuffer contenente il testo di tutti i documenti
     */
    public Map<Document,String> setupReadingPhase() {
        Map<Document,String> result = new HashMap<>();
        for (Document doc : getDocuments()) {
            try {
                result.put(doc, Resources.getDocumentContent(doc.filename()));
            } catch (IOException e) {
                SystemLogger.log("Errore nella lettura del documento", e);
            }
        }
        return result;
    }

    public void saveGameReport(GameReport report) {
        System.out.println("→ Chiamato saveGameReport");
        gameReportDAO.insert(report);
        System.out.println("→ Fine saveGameReport");
    }
}