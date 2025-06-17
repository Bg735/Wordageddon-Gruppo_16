package it.unisa.diem.wordageddon_g16.db;

public class UpdateFailedException extends RuntimeException {
    public UpdateFailedException(String message) {
        super(message);
    }
}
