package it.unisa.diem.wordageddon_g16.db.contracts;

import it.unisa.diem.wordageddon_g16.models.Document;

public interface DocumentDAO extends DAO<Document> {
    boolean isEmpty();
}
