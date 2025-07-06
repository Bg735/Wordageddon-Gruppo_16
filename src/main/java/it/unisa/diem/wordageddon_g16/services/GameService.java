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

            var docList = documentDAO.selectAll();
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
    private Question absoluteFrequencyQuestion() {}

    //metodo che mostra tot. parole e richiede quante volte appare nel documento
    private Question whichMoreQuestion() {}

    //metodo che mostra tot. parole e richiede il numero di occorrenze
    private Question whichLessQuestion() {}

    //metodo che mostra una parola e richiede in quale documento appare
    private Question whichDocumentQuestion() {}

    //metodo che richiede quale parola NON appare in nessun documento
    private  Question witchAbsentQuestion() {}

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
//controllo se params è nullo, creo random prendo per il numero di domande params faccio un for da param.questioncount e per ognuna prendo un tipo da getquestion tipe e da questo genero la domanda di quel tipo
  //questiontype.getrandom type e poi chismo il metodo usando WDM, creo un set e poi aggiungo il valore da wdm e da quello scelgo random la parola set parola- num.occorrenze , genero random la domanda e la risposta
    //getquestion e poi un metoodo per ogni tipo di domanda
    public int getQuestionCount() {
        if (params == null) {
            throw new IllegalStateException("Game not initialized");
        }
        return params.questionCount;
    }

}
