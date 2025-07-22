package it.unisa.diem.wordageddon_g16.db.exceptions;

/**
 * The type Query failed exception.
 */
public class QueryFailedException extends RuntimeException {
    /**
     * Instantiates a new Query failed exception.
     *
     * @param message the message
     */
    public QueryFailedException(String message) {
        super(message);
    }
}
