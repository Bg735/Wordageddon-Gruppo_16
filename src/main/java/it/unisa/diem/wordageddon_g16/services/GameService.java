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

public class GameService {

    private final GameReportDAO gameReportDAO;
    private final WdmDAO wdmDAO;
    private final DocumentDAO documentDAO;
    private final StopWordDAO stopwordDAO;
    private final AppContext context;

    private GameParams params;
    private Map<Document, WDM> wdmMap;

    private class GameParams{

        private static final Random random = new Random();

        private final Duration timer;
        private final List<Document> documents;
        private final int questionCount;
        private final Difficulty difficulty;

        private static class DifficultyIndex {
            private final float cap;
            private float value;

            public DifficultyIndex(float cap) {
                this.cap = cap;
                this.value = cap;
            }

            public float getNext() {
                var result = random.nextFloat(value);
                value -= result;
                return result;
            }

            public float getNextRelative(){
                return getNext() / cap;
            }

            public float getRemaining() {
                return value;
            }

            public float getCap() { return cap; }
        }

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

        private List<Document> generateDocuments(float influence) throws IllegalArgumentException {
            int maxWords = 1000;
            int minWords = 200;
            int wordCountTolerance = 50;

            var result = new ArrayList<Document>();

            var docList = new ArrayList<>(documentDAO.selectAll());
            if (docList.isEmpty()) {
                throw new IllegalStateException("No documents available for the game");
            }

            int wordsNeeded = Math.round(minWords + (maxWords-minWords)*influence);
            var documentsLeft = docList.size();
            do{
                Document currentDoc = docList.get(random.nextInt(documentsLeft--));
                docList.remove(currentDoc);

                int remainder = wordsNeeded - currentDoc.wordCount();
                if (remainder > wordCountTolerance){
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

        private Duration generateTimer(float influence) {
            int timerMax = 10*60; //seconds
            int timerMin = 2*60; //seconds

            return Duration.ofSeconds((long) (timerMax-(timerMax-timerMin)*(influence)));
        }

        private int generateQuestionCount(float influence) {
            int maxQuestions = 20;
            int minQuestions = 5;

            return (int) (minQuestions + (maxQuestions-minQuestions)*influence);
        }
    }

    public record Question(
            String text,
            List<String> answers,
            int correctAnswerIndex
    ){
        enum QuestionType {
            ABSOLUTE_FREQUENCY(1f), // Quante volte appare una parola
            WHICH_MORE(0.5f), // Quale parola appare più spesso tra quelle proposte
            WHICH_LESS(0.5f), // Quale parola appare meno spesso tra quelle proposte
            WHICH_DOCUMENT(1f), // Quale documento contiene una parola
            WHICH_ABSENT(1f); // Quale parola non è presente in nessun documento

            private final float weight;

            QuestionType(float weight) {
                this.weight = weight;
            }

            public static QuestionType getRandomType() {
                var types = values();
                return types[GameParams.random.nextInt(types.length)];
            }
        }

        public static Question create(String text, List<String> answers, int correctAnswerIndex) {
            if (text == null || answers == null || correctAnswerIndex < 0 || correctAnswerIndex >= answers.size()) {
                throw new IllegalArgumentException("Invalid question parameters");
            }
            return new Question(text, answers, correctAnswerIndex);
        }

    }

    public GameService(AppContext context, GameReportDAO gameReportDAO, WdmDAO wdmDAO,
                       DocumentDAO documentDAO, StopWordDAO stopwordDAO) {
        this.gameReportDAO = gameReportDAO;
        this.wdmDAO = wdmDAO;
        this.documentDAO = documentDAO;
        this.stopwordDAO = stopwordDAO;
        this.context = context;
    }


    /**
     * Questo metodo dovrebbe essere chiamato in maniera asincrona, durante la fase di visualizzazione dei documenti:
     * in questo modo, il gioco può iniziare immediatamente, e le domande saranno pronte quando richieste.
     * Tale cautela è dovuta al fatto che la generazione delle domande può richiedere tempo in caso di nuovi documenti,
     * che è necessario scansionare per ricavare domande basate sulle parole contenute al loro interno.
     *
     * @return la lista delle domande da sottoporre durante il quiz, basate sui documenti mostrati all'utente.
     */
    public List<Question> getQuestions() {
        if (params == null) throw new IllegalStateException("Game not initialized");
        loadWdmMap();
        List<Question> questions = new ArrayList<>();
        for(int i = 0; i < params.questionCount; i++) {
            var type = Question.QuestionType.getRandomType();
            Question q = switch (type) {
                case ABSOLUTE_FREQUENCY -> absoluteFrequencyQuestion();
                case WHICH_MORE -> whichMoreQuestion();
                case WHICH_LESS -> whichLessQuestion();
                case WHICH_DOCUMENT -> whichDocumentQuestion();
                case WHICH_ABSENT -> witchAbsentQuestion();
            };
            questions.add(q);
        }
    return questions;

    }

    //metodo che mostra una parola e richiede quante volte appare nel doc
    private Question absoluteFrequencyQuestion() {
        List<Document> docs  = params.documents;
        Document document = docs.get(GameParams.random.nextInt(docs.size()));
        WDM wdm = wdmMap.get(document); //wdm associata al documento

        //estraggo le parole presenti nel document
        List<String> words = new ArrayList<>();
        for (String word : wdm.getWords().keySet()) {
            words.add(word);
        }
        //controllo che ci sia almeno 1 parola
        if(words.isEmpty()) throw new IllegalStateException("No words available");

        //scelgo una parola random e vedo quante volte appare nel document
        String word = words.get(GameParams.random.nextInt(words.size()));
        int frequency = wdm.getWords().get(word);

        //genero le restanti (3) risposte sbagliate
        Set<Integer> wrongAnswers = new HashSet<>();
        while(wrongAnswers.size() < 3) {
            int answerIndex = frequency  + GameParams.random.nextInt(5) - 2;
            if(answerIndex != frequency && answerIndex > 0) {
                wrongAnswers.add(answerIndex);
            }
        }
        //lista finale delle risposte
        List<String> answers = new ArrayList<>();
        for(int answerIndex : wrongAnswers) {
            answers.add(String.valueOf(answerIndex));
        }
        int correctAnswerIndex = GameParams.random.nextInt(4);
        answers.add(correctAnswerIndex, String.valueOf(frequency));
        return Question.create("Quante volte appare la parola " + word + "nel documento " + document.title() + "?", answers, correctAnswerIndex);
    }

    //metodo che mostra tot. parole e richiede quale parola appare più frequentemente
    private Question whichMoreQuestion() {
        List<Document> docs = params.documents;
        Document document = docs.get(GameParams.random.nextInt(docs.size()));
        WDM wdm = wdmMap.get(document);

        //ottengo le parole + la rispettiva frequenza del doc
        List<Map.Entry<String, Integer>> wordFrequency = new ArrayList<>(wdm.getWords().entrySet());
        Collections.shuffle(wordFrequency);

        //seleziono le prime 4 parole della lista
        List<Map.Entry<String, Integer>> currentAnswer = new ArrayList<>();
        for(int y = 0; y < 4; y++) {
          currentAnswer.add(wordFrequency.get(y)) ;
        }

        //lista parole da usare come risposta
        List<String> answers = new ArrayList<>();
        int correctIndex = 0;
        int maxFreq = -1;

        //aggiungo parola alla risposta
        for (int i = 0; i < currentAnswer.size(); i++) {
            Map.Entry<String, Integer> entry = currentAnswer.get(i);
            answers.add(entry.getKey());

        //controllo se la parola ha la frequenza più alta
        if(entry.getValue() > maxFreq) {
        maxFreq = entry.getValue();
        correctIndex = i;
        }
        }

        //creo la domanda
        return Question.create( "Quale di queste parole appare più frequentemente nel documento " + document.title() + "?", answers, correctIndex);
    }

    //metodo che mostra tot. parole e richiede quale appare meno spesso
    private Question whichLessQuestion() {
        List<Document> docs  = params.documents;
        Document document = docs.get(GameParams.random.nextInt(docs.size()));
        WDM wdm = wdmMap.get(document);

        //ottengo le parole + la rispettiva frequenza del doc
        List<Map.Entry<String, Integer>> wordFrequency = new ArrayList<>(wdm.getWords().entrySet());
        Collections.shuffle(wordFrequency);

        //copio i primi 4 elementi in una nuova lista
        List<Map.Entry<String, Integer>> selected = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            selected.add(wordFrequency.get(i));
        }

        //indice della risposta + valore minimo della frequenza
        int correctIndex = 0;
        int minFreq = Integer.MAX_VALUE;

        //scorro la lista per trovare la frequenza minima
        for(int i = 0; i<selected.size(); i++) {
            if(selected.get(i).getValue() < minFreq) {
                minFreq = selected.get(i).getValue();
                correctIndex = i;
            }
        }

        //creo la solita lista finale delle risposte
        List<String> answers = new ArrayList<>();
        for (var entry : selected) {
            answers.add(entry.getKey());
        }

        //creo la domanda
        return Question.create("Quale delle seguenti parole appare meno frequentemente nel documento " + document.title() + "?", answers, correctIndex);
    }

    //metodo che mostra una parola e richiede in quale documento appare
    private Question whichDocumentQuestion() {
        List<Document> docs  = params.documents;
        Document document = docs.get(GameParams.random.nextInt(docs.size()));
        WDM wdm = wdmMap.get(document);

        //estraggo le parole dal doc scelto
        List<String> words = new ArrayList<>(wdm.getWords().keySet());
        if(words.isEmpty()) throw new IllegalStateException("No words available");

        //scelgo una parola casuale da quel doc
        String word = words.get(GameParams.random.nextInt(words.size()));

        //lista d documenti da usare come risposte
        List<Document> docAnswer = new ArrayList<>();
        docAnswer.add(document);
        while(docAnswer.size() < 4) {
            Document d = docs.get(GameParams.random.nextInt(docs.size()));
            docAnswer.add(d);
        }
        Collections.shuffle(docAnswer);

        //creo lista delle risposte e identifico l'indice corretto
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

    //metodo che richiede quale parola NON appare in nessun documento
    private  Question whichAbsentQuestion() {
        List<Document> docs  = params.documents;
        //costruisco un set con tutte le parole presenti nei documenti
        Set<String> allWords = new HashSet<>();
        for (WDM wdm : wdmMap.values()) {
            allWords.addAll(wdm.getWords().keySet());
        }
        if (allWords.size() < 3) {
            throw new IllegalStateException("Not enough words for the question");
        }

        //seleziono 3 parole che sono presenti nel doc
        List<String> presentWords = new ArrayList<>(allWords);
        Collections.shuffle(presentWords);
        List<String> answers = new ArrayList<>();
        answers.add(presentWords.get(0));
        answers.add(presentWords.get(1));
        answers.add(presentWords.get(2));

        //invento parola
        String absentWord = "rossella";
        answers.add(absentWord);

        //mischio le risposte
        Collections.shuffle(answers);
        int correctIndex = answers.indexOf(absentWord);

        return Question.create( "Quale delle seguenti parole NON è presente in nessun documento?", answers, correctIndex);
    }

    private void loadWdmMap(){
        for(Document doc : params.documents){
            WDM wdm;
            var optionalWdm = wdmDAO.selectById(doc);
            if (optionalWdm.isEmpty()){
                wdm = new WDM(doc, stopwordDAO.selectAll());
                wdmDAO.insert(wdm);
            }
            else wdm = optionalWdm.get();
            wdmMap.put(doc,wdm);
        }
    }

    public void init(Difficulty difficulty) {
        params = new GameParams(difficulty);
        wdmMap = new HashMap();
    }

    public Difficulty getDifficulty() {
        if (params == null) {
            throw new IllegalStateException("Game not initialized");
        }
        return params.difficulty;
    }

    public Duration getTimeLimit() {
        if (params == null) {
            throw new IllegalStateException("Game not initialized");
        }
        return params.timer;
    }

    public List<Document> getDocuments() {
        if (params == null) {
            throw new IllegalStateException("Game not initialized");
        }
        return params.documents;
    }

    public int getQuestionCount() {
        if (params == null) {
            throw new IllegalStateException("Game not initialized");
        }
        return params.questionCount;
    }

}
