package it.unisa.diem.wordageddon_g16.db.contracts;

import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.WDM;

import java.util.Optional;

public interface WdmDAO extends DAO<WDM> {
    Optional<WDM> selectBy(Document document);
}
