package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.db.contracts.DocumentDAO;
import it.unisa.diem.wordageddon_g16.db.exceptions.QueryFailedException;
import it.unisa.diem.wordageddon_g16.db.exceptions.UpdateFailedException;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.utility.SystemLogger;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Implementazione JDBC del {@link DocumentDAO}, che gestisce le operazioni sui documenti.
 * <p>
 * I documenti sono salvati nella tabella {@code Document} e rappresentati tramite il model {@link Document}.
 * Tutte le interazioni con il database sono gestite tramite {@link JdbcDAO}, con logging automatico via {@link SystemLogger}.
 */
public class JDBCDocumentDAO extends JdbcDAO<Document> implements DocumentDAO {

    /**
     * Costruisce un nuovo DocumentDAO utilizzando la connessione specificata.
     *
     * @param conn la connessione al database da utilizzare per le operazioni
     */
    public JDBCDocumentDAO(Connection conn) {
        super(conn);
    }

    /**
     * Recupera un documento dal database in base al suo identificativo (rappresentato dal {@code filename}).
     * <p>
     * Se il documento esiste, viene restituito incapsulato in un {@link Optional}. Se non esiste, il risultato sarà vuoto.
     *
     * @param filename identificativo univoco del documento
     * @return {@code Optional} contenente il documento trovato, oppure vuoto
     * @throws QueryFailedException se l'esecuzione della query fallisce
     */
    @Override
    public Optional<Document> selectBy(String filename) {
        String query = "SELECT * FROM Document WHERE id = ?";
        Callback<ResultSet, Optional<Document>> callback = res -> {
            try {
                if (res != null && res.next()) {
                    Document document = new Document(
                            res.getString("id"),
                            res.getString("title"),
                            res.getInt("word_count")
                    );
                    return Optional.of(document);
                }
            } catch (SQLException e) {
                SystemLogger.log("Error trying to get document by id", e);
                throw new QueryFailedException(e.getMessage());
            }
            return Optional.empty();
        };
        return executeQuery(query, callback, filename);
    }


    /**
     * Recupera tutti i documenti presenti nella tabella {@code Document}.
     *
     * @return lista di {@link Document} recuperati dal database
     * @throws QueryFailedException se l'esecuzione della query fallisce
     */
    @Override
    public List<Document> selectAll() {
        String query = "SELECT * FROM Document";
        Callback<ResultSet,List<Document>> callback = res -> {
            try {
                if (res == null) {
                    return List.of();
                }
                var result = new java.util.ArrayList<Document>();
                while (res.next()) {
                    result.add(new Document(
                            res.getString("id"),
                            res.getString("title"),
                            res.getInt("word_count")
                    ));
                }
                return result;
            } catch (Exception e) {
                SystemLogger.log("Error trying to get all documents", e);
                throw new QueryFailedException(e.getMessage());
            }
        };
        return executeQuery(query, callback);
    }

    /**
     * Inserisce un nuovo {@link Document} nella tabella. Se il documento esiste già (stesso {@code id}), l'operazione viene ignorata.
     *
     * @param document documento da inserire
     * @throws QueryFailedException se si verifica un errore durante l'inserimento
     */
    @Override
    public void insert(Document document) {
        String query = "INSERT OR IGNORE INTO Document (title, id, word_count) VALUES (?, ?, ?)";
        try {
            executeUpdate(query, document.title(), document.filename(), document.wordCount());
        } catch (Exception e) {
            SystemLogger.log("Error trying to insert document: " + document, e);
            throw new QueryFailedException(e.getMessage());
        }
    }

    /**
     * Aggiorna i dati di un {@link Document} esistente nella tabella, modificandone titolo e numero di parole.
     *
     * @param document documento da aggiornare
     * @throws UpdateFailedException se si verifica un errore durante l’aggiornamento
     */
    @Override
    public void update(Document document) {
        String query = "UPDATE Document SET title = ?, word_count = ? WHERE id = ?";
        try {
            executeUpdate(query, document.title(), document.wordCount(), document.filename());
        } catch (Exception e) {
            SystemLogger.log("Error trying to update document: " + document, e);
            throw new UpdateFailedException(e.getMessage());
        }
    }

    /**
     * Elimina un {@link Document} dalla tabella.
     * <p>
     * Grazie ai vincoli di integrità {@code ON DELETE CASCADE}, vengono rimossi anche i record associati (es. {@code Content}, {@code WDM}).
     *
     * @param document documento da eliminare
     * @throws UpdateFailedException se l'eliminazione fallisce
     */
    @Override
    public void delete(Document document) {
        String query = "DELETE FROM Document WHERE id = ?";
        try {
            executeUpdate(query, document.filename());
        } catch (Exception e) {
            SystemLogger.log("Error trying to delete document: " + document, e);
            throw new UpdateFailedException(e.getMessage());
        }
    }

    /**
     * Verifica se la tabella {@code Document} contiene almeno una riga.
     *
     * @return {@code true} se la tabella è vuota, {@code false} altrimenti
     */
    @Override
    public boolean isEmpty() {
        return super.isEmpty("Document");
    }
}