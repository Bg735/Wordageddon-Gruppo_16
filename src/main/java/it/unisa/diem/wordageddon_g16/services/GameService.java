package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.DocumentDAO;
import it.unisa.diem.wordageddon_g16.db.GameReportDAO;
import it.unisa.diem.wordageddon_g16.db.StopWordDAO;
import it.unisa.diem.wordageddon_g16.db.WdmDAO;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Difficulty;
import it.unisa.diem.wordageddon_g16.models.Document;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameService {

    private final GameReportDAO gameReportDAO;
    private final WdmDAO wdmDAO;
    private final DocumentDAO documentDAO;
    private final StopWordDAO stopwordDAO;
    private final AppContext context;

    public class GameParams{

        private static final Random random = new Random();

        private Duration timer;
        private List<Document> documents;
        private int questionCount;

        private class DifficultyIndex {
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


        public GameParams(Difficulty difficulty) {

            var cap = switch (difficulty) {
                case EASY -> 1.0f;
                case MEDIUM -> 2.0f;
                case HARD -> 3.0f;
            };
            var di = new DifficultyIndex(cap);
            documents = generateDocuments(di);
            timer = generateTimer(di);

        }
        private List<Document> generateDocuments(DifficultyIndex di) throws IllegalArgumentException {
            int maxWords = 1000;
            int minWords = 200;
            int wordCountTolerance = 50;

            var result = new ArrayList<Document>();

            var docList = documentDAO.selectAll();
            if (docList.isEmpty()) {
                throw new IllegalStateException("No documents available for the game");
            }

            int wordsNeeded = Math.round(minWords + (maxWords-minWords)*di.getNextRelative());
            var documentsLeft = docList.size();
            do{
                Document currentDoc = docList.get(random.nextInt(documentsLeft--));
                docList.remove(currentDoc);

                int remainder = wordsNeeded - currentDoc.getWordCount();
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

        private Duration generateTimer(DifficultyIndex di) {
            int timerMax = 10*60; //seconds
            int timerMin = 2*60; //seconds

            return Duration.ofSeconds((long) (timerMax-(timerMax-timerMin)*(di.getNextRelative())));
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



}
