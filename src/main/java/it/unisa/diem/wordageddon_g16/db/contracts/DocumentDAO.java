package it.unisa.diem.wordageddon_g16.db.contracts;

import it.unisa.diem.wordageddon_g16.models.Document;

import java.util.Optional;

public interface DocumentDAO extends DAO<Document> {
    Optional<Document> selectBy(String filename);
    boolean isEmpty();
}
