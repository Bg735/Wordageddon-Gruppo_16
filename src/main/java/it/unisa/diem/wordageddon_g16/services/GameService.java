package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.JDBCGameReportDAO;
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
 * Classe di "servizio" per la gestione della logica di gioco in Wordageddon.
 * Fornisce tutte le funzionalità utili al GameController come per inizializzare una partita, generare domande,
 * calcolare punteggi, gestire documenti e salvare i risultati.
 */

public class GameService {
    private final GameReportDAO gameReportDAO;
    private final JDBCWdmDAO wdmDAO;
    private final DocumentDAO documentDAO;
    private final StopWordDAO stopWordDAO;
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
        this.context = context;
        this.gameReportDAO = gameReportDAO;
        this.wdmDAO = wdmDAO;
        this.documentDAO = documentDAO;
        this.stopWordDAO = stopwordDAO;
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
     * Genera la lista di domande per la sessione di gioco in base ai documenti e alla difficoltà selezionata.
     * <p>
     * Se {@link #params} non è inizializzato, viene sollevata una {@link IllegalStateException}.
     * Il metodo definisce i tipi di domanda ammessi in base al numero di documenti disponibili.
     * </p>
     * <ul>
     *   <li>Con meno di 4 documenti: solo domande di tipo SINGLE, ossia basate su un singolo documento</li>
     *   <li>Con 4 o più documenti: include anche {@link Question.QuestionType#WHICH_DOCUMENT} e {@link Question.QuestionType#WHICH_ABSENT}, ossia domande che chiedono una parola in che documento è presente di più o in quale è assente</li>
     * </ul>
     * <p>
     * Per ogni domanda da generare:
     * <ul>
     *   <li>Seleziona casualmente il {@link Question.QuestionType}</li>
     *   <li>Chiama il metodo corrispondente come {@code absoluteFrequencyQuestion()} o {@code whichMoreQuestionSingle()}</li>
     * </ul>
     * </p>
     *
     * @return lista di {@link Question} generate per la sessione attuale
     * @throws IllegalStateException se il gioco non è stato inizializzato correttamente
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
            System.out.println("Il sistema ha selezionato meno di 4 documenti, le domande generate saranno di tipo SINGLE (riguardano un singolo documento)");
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

    /**
     * Genera una domanda {@link Question} sulla frequenza assoluta di una parola in un singolo documento.
     * <p>
     * Seleziona casualmente un {@link Document}, recupera la sua {@link WDM} associata e sceglie una parola presente.
     * Crea quattro opzioni numeriche plausibili e identifica quella corretta in base alla frequenza della parola nel documento.
     * </p>
     *
     * @return domanda a scelta multipla relativa alla frequenza di una parola in un singolo documento
     */
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
                "Quante volte la parola \"" + chosenWord.toUpperCase() + "\" appare nel documento \"" + document.title().toUpperCase() + "\"?",
                answers,
                correctIndex
        );
    }

    /**
     * Genera una domanda {@link Question} sulla frequenza assoluta di una parola in tutti i documenti combinati.
     * <p>
     * Somma le frequenze di tutte le parole attraverso i {@link WDM} dei documenti.
     * Seleziona una parola casuale e genera opzioni di risposta basate sulla sua frequenza cumulata.
     * </p>
     *
     * @return domanda relativa alla frequenza di una parola aggregata su tutti i documenti
     * @throws IllegalStateException se non ci sono parole disponibili nei documenti
     */
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

    /**
     * Genera una domanda {@link Question} in cui si chiede quale parola appare più frequentemente tra un insieme proposto, basata su tutti i documenti.
     * <p>
     * Accumula le frequenze totali per ogni parola e seleziona casualmente quattro parole tra quelle disponibili.
     * Identifica quella con la frequenza più alta come risposta corretta.
     * </p>
     *
     * @return domanda a scelta multipla sulla parola con frequenza massima complessiva
     * @throws IllegalStateException se il numero di parole disponibili è inferiore a 4
     */
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

    /**
     * Genera una domanda {@link Question} in cui si chiede quale parola appare più frequentemente in un singolo documento.
     * <p>
     * Seleziona casualmente un {@link Document} e sceglie quattro parole dalla sua {@link WDM}.
     * Identifica la parola con frequenza più alta come risposta corretta.
     * </p>
     *
     * @return una Question a scelta multipla relativa alla parola più frequente nel documento selezionato
     */
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
        return Question.create("Quale di queste parole appare più frequentemente nel documento \"" + document.title().toUpperCase() + "\"?", answers, correctIndex);
    }
    /**
     * Genera una domanda {@link Question} in cui si chiede quale parola appare meno frequentemente in un singolo documento.
     * <p>
     * Seleziona un {@link Document} casuale e quattro parole dalla sua {@link WDM}.
     * Individua quella con la frequenza più bassa come risposta corretta.
     * </p>
     *
     * @return domanda sulla parola con minore frequenza in un documento specifico
     */
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
        return Question.create("Quale delle seguenti parole appare meno frequentemente nel documento \"" + document.title().toUpperCase() + "\"?", answers, correctIndex);
    }

    /**
     * Genera una domanda {@link Question} che richiede di identificare la parola meno frequente
     * tra un insieme di quattro, basata sui dati cumulativi di tutti i documenti.
     * <p>
     * Combina le frequenze di tutte le parole usando le rispettive {@link WDM},
     * ne seleziona quattro casualmente e individua quella con la frequenza più bassa.
     * </p>
     *
     * @return domanda a scelta multipla sulla parola con minore frequenza globale
     * @throws IllegalStateException se non sono disponibili abbastanza dati per la generazione
     */
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

    /**
     * Genera una domanda {@link Question} che richiede di identificare
     * in quale documento appare una determinata parola.
     * <p>
     * Seleziona un {@link Document} casuale e una parola presente al suo interno.
     * Prepara un insieme di documenti tra cui scegliere, garantendo che il documento corretto sia incluso,
     * e costruisce le opzioni di risposta in ordine casuale.
     * </p>
     *
     * @return domanda sulla presenza di una parola in uno dei documenti disponibili
     * @throws IllegalStateException se il documento selezionato non contiene parole
     */
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
    /**
     * Genera una domanda {@link Question} che richiede di identificare
     * quale parola tra quattro non è presente in nessun documento.
     * <p>
     * Recupera tutte le parole effettivamente contenute nei documenti tramite le rispettive {@link WDM}.
     * Ne seleziona tre esistenti e ne genera una quarta che non compare in alcun documento, utilizzando {@code generateAbsentWord()}.
     * </p>
     *
     * @return domanda che verifica l'assenza totale di una parola nei documenti
     * @throws IllegalStateException se non ci sono abbastanza parole per generare la domanda
     */
    private Question whichAbsentQuestion() {
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
     * Genera una parola che non è presente in alcuno dei documenti selezionati per la partita.
     * <p>
     * Il metodo tenta di prelevare una parola da:
     * <ul>
     *   <li>Documenti inutilizzati: estrae una parola dalla {@link WDM} associata a un documento non usato, filtrando quelle già presenti.</li>
     *   <li>Vocabolario statico: se non ci sono documenti inutilizzati o nessuna parola valida, seleziona una parola casuale da {@link Resources#getVocabulary()}.</li>
     * </ul>
     * Se non trova alcuna parola valida, lancia una {@link IllegalStateException}.
     * </p>
     *
     * @param presentWords insieme di parole già presenti nei documenti usati
     * @return una parola assente da tutti i documenti utilizzati
     * @throws IllegalStateException se non ci sono parole disponibili né nei documenti inutilizzati né nel vocabolario statico
     */
    private String generateAbsentWord(Set<String> presentWords) {
        // True: la parola è prelevata da un vocabolario statico
        // False: la parola è prelevata dalla WDM di un documento non utilizzato durante la partita
        boolean useVocabulary = false;

        // Creo una lista di documenti non utilizzati
        List<Document> usedDocsList = getDocuments();
        List<Document> allDocsList = documentDAO.selectAll().stream().toList();
        List<Document> unusedDocsList = allDocsList.stream()
                .filter(doc -> !usedDocsList.contains(doc))
                .collect(Collectors.toList());

        // Se non ci sono documenti inutilizzati, uso il vocabolario statico
        if (unusedDocsList.isEmpty()) {
            System.out.println("Nessun documento inutilizzato, uso il vocabolario statico");
            useVocabulary = true;
        }

        String word = null;

        // Prelevo la parola da un documento inutilizzato
        if (!useVocabulary) {
            Collections.shuffle(unusedDocsList);

            // Trova una parola valida da una WDM di un documento inutilizzato
            for (Document unusedDoc : unusedDocsList) {
                WDM unusedDocWdm = wdmMap.get(unusedDoc);
                if (unusedDocWdm == null) continue;

                List<String> unusedDocWords = new ArrayList<>(unusedDocWdm.getWords().keySet());

                // Rimuovo le parole già presenti nei documenti utilizzati
                unusedDocWords.removeIf(presentWords::contains);

                if (!unusedDocWords.isEmpty()) {
                    // Prelevo una parola casuale da quelle rimaste nella collezione
                    Collections.shuffle(unusedDocWords);
                    word = unusedDocWords.getFirst();
                    break;
                }
            }

            // Se non trovata, fallback al vocabolario statico
            if (word == null) {
                useVocabulary = true;
            }
        }

        if (useVocabulary) {
            // Prelevo una parola dal vocabolario statico
            // Bisogna creare una nuova lista in quanto quella restituita da Resources.getVocabulary() é immutabile
            // avendola ottenuta mediante il costruttore Arrays.
            List<String> vocabWords = new ArrayList<>(Resources.getVocabulary());
            vocabWords.removeIf(presentWords::contains);
            if (vocabWords.isEmpty()) {
                throw new IllegalStateException("Nessuna parola disponibile nel vocabolario statico!");
            }
            Collections.shuffle(vocabWords);
            word = vocabWords.getFirst();
        }

        System.out.println("Generated Word: " + word);
        return word;

    }

    /**
     * Carica le matrici {@link WDM} associate ai documenti selezionati per la partita nella mappa {@code wdmMap}.
     * <p>
     * Per ciascun {@link Document} in {@code params.documents}, il metodo:
     * <ul>
     *   <li>Recupera la matrice dal database tramite {@code wdmDAO.selectBy(Document)}</li>
     *   <li>La inserisce nella mappa {@code wdmMap}</li>
     *   <li>Se la matrice non è disponibile, viene lanciata una {@link IllegalStateException}</li>
     * </ul>
     * </p>
     */
    private void loadWdmMap() {
        for (Document doc : params.documents) {
            WDM wdm;
            var optionalWdm = wdmDAO.selectBy(doc);
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
     * Rappresenta una domanda a risposta multipla generata dal {@link GameService}.
     *
     * <p>
     * Ogni {@code Question} contiene:
     * <ul>
     *   <li>Il testo della domanda ({@code text})</li>
     *   <li>Un elenco di possibili risposte ({@code answers})</li>
     *   <li>L'indice della risposta corretta ({@code correctAnswerIndex})</li>
     * </ul>
     * Utilizzato nella fase quiz per testare la comprensione dell'utente sui documenti letti.
     */
    public record Question(
            String text,
            List<String> answers,
            int correctAnswerIndex
    ) {
        /**
         * Enum interno che definisce le diverse tipologie di domande generabili.
         * <p>
         * Ogni tipo ha un {@code weight} che ne indica la rilevanza nella fase di generazione.
         * Tipologie disponibili:
         * <ul>
         *   <li>{@code ABSOLUTE_FREQUENCY} – Quante volte appare una parola</li>
         *   <li>{@code WHICH_MORE} – Quale parola appare più spesso</li>
         *   <li>{@code WHICH_LESS} – Quale parola appare meno spesso</li>
         *   <li>{@code WHICH_DOCUMENT} – In quale documento compare una parola</li>
         *   <li>{@code WHICH_ABSENT} – Quale parola è assente da tutti i documenti</li>
         * </ul>
         */
        public enum QuestionType {
            ABSOLUTE_FREQUENCY(1f), // Quante volte appare una parola
            WHICH_MORE(0.5f),       // Quale parola appare più spesso tra quelle proposte
            WHICH_LESS(0.5f),       // Quale parola appare meno spesso tra quelle proposte
            WHICH_DOCUMENT(1f),     // Quale documento contiene una parola
            WHICH_ABSENT(1f);       // Quale parola non è presente in nessun documento

            private final float weight;
            /**
             * Costruisce una tipologia di domanda con peso associato.
             *
             * @param weight valore numerico che rappresenta il peso logico del tipo di domanda
             */
            QuestionType(float weight) {
                this.weight = weight;
            }
        }
        /**
         * Crea una nuova istanza di {@code Question} validando i parametri forniti.
         * <p>
         * La domanda è valida solo se:
         * <ul>
         *   <li>{@code text} non è {@code null}</li>
         *   <li>{@code answers} non è {@code null}</li>
         *   <li>{@code correctAnswerIndex} è compreso tra {@code 0} e {@code answers.size() - 1}</li>
         * </ul>
         * Se non rispettati, viene lanciata una {@link IllegalArgumentException}.
         * </p>
         *
         * @param text               testo della domanda
         * @param answers            lista delle possibili risposte
         * @param correctAnswerIndex indice della risposta corretta
         * @return istanza valida di {@code GameService.Question}
         * @throws IllegalArgumentException se i parametri sono invalidi
         */
        public static Question create(String text, List<String> answers, int correctAnswerIndex) {
            if (text == null || answers == null || correctAnswerIndex < 0 || correctAnswerIndex >= answers.size()) {
                throw new IllegalArgumentException("Invalid question parameters");
            }
            return new Question(text, answers, correctAnswerIndex);
        }
    }

    /**
     * Calcola il punteggio assegnato per ogni singola domanda in base alla
     * difficoltà della partita e al numero totale di domande.
     *
     * @return il punteggio per ogni domanda
     */
    public int getScorePerQuestion() {
        int totalScore = Difficulty.getMaxScoreDifficulty(getDifficulty());
        return totalScore / getQuestionCount();
    }


    /**
     * Classe interna che incapsula i parametri di una partita.
     * <p>
     * Rappresenta i parametri generati automaticamente per una partita in corso in base alla difficoltà scelta.
     * Contiene difficoltà, timer, documenti selezionati e numero di domande.
     * </p>
     */
    private class GameParams {
        private static final Random random = new Random();
        private final Duration timer;
        private final List<Document> documents;
        private final int questionCount;
        private final Difficulty difficulty;

        /**
         * Classe di supporto per la gestione della difficoltà.
         * <p>
         *     Calcola in modo progressivo l'influenza della difficoltà su vari parametri
         * </p>
         */
        private static class DifficultyIndex {
            private float value;

            /**
             * Costruisce un nuovo DifficultyIndex.
             */
            public DifficultyIndex() {
                this.value = 1;
            }

            /**
             * Restituisce un valore casuale in base alla difficoltà corrente,
             * e lo sottrae dal valore disponibile.
             *
             * @return valore parziale generato casualmente
             */
            public float getNext() {
                var result = random.nextFloat(value);
                value -= result;
                return result;
            }

            /**
             * Fornisce la quantità di difficoltà ancora disponibile.
             *
             * @return valore rimanente
             */
            public float getRemaining() {
                return value;
            }
        }

        /**
         * Costruisce i parametri di gioco basati sul livello di difficoltà.
         * Usa {@link DifficultyIndex} per calcolare le componenti e chiama:
         * {@link #generateDocuments(float)},
         * {@link #generateTimer(float)},
         * {@link #generateQuestionCount(float)}
         *
         * @param difficulty livello di difficoltà selezionato
         */
        private GameParams(Difficulty difficulty) {
            this.difficulty = difficulty;
            var di = new DifficultyIndex();
            documents = Collections.unmodifiableList(generateDocuments(di.getNext()));
            timer = generateTimer(di.getNext());
            questionCount = generateQuestionCount(di.getRemaining());
        }

        /**
         * Genera una lista di documenti in base all'influenza della difficoltà.
         *<p>
         * Questo metodo viene invocato nel costruttore {@link GameParams#GameParams(Difficulty)}.
         *</p>
         * @param influence valore di influenza della difficoltà
         * @return lista di documenti
         * @throws IllegalArgumentException se non sono disponibili documenti
         */
        private List<Document> generateDocuments(float influence) throws IllegalArgumentException {
            final int maxWords;
            final int minWords;
            final int maxDocsNumber;
            final int wordCountTolerance = 100;

            // Classificazione dei documenti in base al numero delle parole
            // In base alla difficoltá scelta, si prelevano i documenti con un numero di parole compreso tra min e max
            switch (difficulty) {
                case EASY -> {
                    maxWords = 250;
                    minWords = 50;
                }
                case MEDIUM -> {
                    maxWords = 350;
                    minWords = 200;
                }
                case HARD -> {
                    maxWords = 450;
                    minWords = 300;
                }
                default -> throw new IllegalArgumentException("Invalid difficulty level");
            }

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
         * Genera la durata della sessione in base all'influenza della difficoltà.
         *<p>
         * Metodo richiamato da {@link GameParams#GameParams(Difficulty)}.
         *</p>
         * @param influence fattore che determina la durata del timer
         * @return {@code Duration} impostata
         */
        private Duration generateTimer(float influence) {
            int timerMax = 10 * 60; // secondi
            int timerMin = 2 * 60; // secondi

            return Duration.ofSeconds((long) (timerMax - (timerMax - timerMin) * (influence)));
        }

        /**
         * Genera il numero di domande per la partita in base all'influenza della difficoltà.
         *<p>
         * Questo metodo viene invocato nel costruttore {@link GameParams#GameParams(Difficulty)}.
         *</p>
         * @param influence valore di influenza della difficoltà
         * @return numero di domande
         */
        private int generateQuestionCount(float influence) {
            final int maxQuestions;
            final int minQuestions;

            switch (difficulty) {
                case EASY -> {
                    maxQuestions = 5;
                    minQuestions = 2;
                }
                case MEDIUM -> {
                    maxQuestions = 10;
                    minQuestions = 4;
                }
                case HARD -> {
                    maxQuestions = 15;
                    minQuestions = 5;
                }
                default -> throw new IllegalArgumentException("Invalid difficulty level");
            }

            // per come é stato impostato l'influenza é un valore compreso tra 0 e 1 quindi il massimale scritto non si raggiunge mai
            return Math.round(minQuestions + (maxQuestions - minQuestions) * influence);
        }
    }

    /**
     * Prepara il contenuto testuale dei documenti per la fase di lettura.
     * <p>
     * Per ogni {@link Document} restituito da {@link #getDocuments()}, legge il contenuto del file corrispondente
     * tramite {@link Resources#getDocumentContent(String)} usando il nome fornito da {@link Document#filename()}.
     * <br>
     * In caso di errore nella lettura di un file, registra l'eccezione con {@code SystemLogger.log()}.
     * </p>
     *
     * @return mappa contenente ogni {@code Document} e il suo contenuto testuale pronto per essere visualizzato
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

    /**
     * Salva il report di gioco.
     * <p>
     * Viene chiamato dal GameController a fine partita per registrare i dati finali del giocatore utilizzando {@link JDBCGameReportDAO#insert(GameReport)}.
     * Il report include informazioni su punteggio, tempo di registrazione, difficoltà, tempo massimo di gioco, tempo utilizzato, documenti utilizzati.
     * </p>
     *
     * @param report oggetto {@code GameReport} da salvare
     */
    public void saveGameReport(GameReport report) {
        System.out.println("Salvataggio Report");
        gameReportDAO.insert(report);
    }
}