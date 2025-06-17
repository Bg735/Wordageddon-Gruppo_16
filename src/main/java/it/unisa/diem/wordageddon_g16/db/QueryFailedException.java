package it.unisa.diem.wordageddon_g16.db;

public class QueryFailedException extends RuntimeException {
    public QueryFailedException(String message) {
        super(message);
    }
}
