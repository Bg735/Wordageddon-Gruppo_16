package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.db.exceptions.QueryFailedException;
import it.unisa.diem.wordageddon_g16.db.exceptions.UpdateFailedException;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import javafx.util.Callback;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) per la gestione dei documenti nel database.
 * Consente di eseguire operazioni CRUD sulla tabella Document,
 * rappresentando ogni documento come un oggetto {@link Document}.
 */
public class DocumentDAO extends JdbcDAO<Document> {

    /**
     * Costruisce un nuovo DocumentDAO utilizzando la connessione specificata.
     *
     * @param conn la connessione al database da utilizzare per le operazioni
     */
    public DocumentDAO(Connection conn) {
        super(conn);
    }

    /**
     * Recupera un documento dal database tramite il suo identificativo.
     *
     * @param oPath l'identificativo del documento da recuperare
     * @return un Optional contenente il documento trovato, o vuoto se non esiste
     * @throws QueryFailedException se si verifica un errore durante la query
     */
    @Override
    public Optional<Document> selectById(Object oPath) {
        Path path = (Path) oPath;
        String query = "SELECT * FROM Document WHERE path = ?";
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
                SystemLogger.log("Error trying to get document by filename", e);
                throw new QueryFailedException(e.getMessage());
            }
            return Optional.empty();
        };

        return executeQuery(query, callback, path.toString());
    }


    /**
     * Recupera tutti i documenti presenti nella tabella Document.
     *
     * @return una lista di tutti i documenti nel database
     * @throws QueryFailedException se si verifica un errore durante la query
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
                            res.getString("title"),
                            Path.of(res.getString("path")),
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
     * Inserisce un nuovo documento nella tabella Document.
     *
     * @param document il documento da inserire
     * @throws QueryFailedException se si verifica un errore durante l'inserimento
     */
    @Override
    public void insert(Document document) {
        String query = "INSERT INTO Document (title, path, word_count) VALUES (?, ?, ?)";
        try {
            executeUpdate(query, document.title(), document.path(), document.wordCount());
        } catch (Exception e) {
            SystemLogger.log("Error trying to insert document: " + document, e);
            throw new QueryFailedException(e.getMessage());
        }
    }

    /**
     * Aggiorna le informazioni di un documento esistente nella tabella Document.
     *
     * @param document il documento da aggiornare
     * @throws UpdateFailedException se si verifica un errore durante l'aggiornamento
     */
    @Override
    public void update(Document document) {
        String query = "UPDATE Document SET title = ?, word_count = ? WHERE path = ?";
        try {
            executeUpdate(query, document.title(), document.wordCount(), document.path().toString());
        } catch (Exception e) {
            SystemLogger.log("Error trying to update document: " + document, e);
            throw new UpdateFailedException(e.getMessage());
        }
    }

    /**
     * Elimina un documento dalla tabella Document.
     * L'eliminazione di un documento comporta anche l'eliminazione delle entità associate (ad esempio Content e WDM)
     * grazie ai vincoli di integrità (ON DELETE CASCADE).
     *
     * @param document il documento da eliminare
     * @throws UpdateFailedException se si verifica un errore durante l'eliminazione
     */
    @Override
    public void delete(Document document) {
        String query = "DELETE FROM Document WHERE path = ?";
        try {
            executeUpdate(query, document.path().toString());
        } catch (Exception e) {
            SystemLogger.log("Error trying to delete document: " + document, e);
            throw new UpdateFailedException(e.getMessage());
        }
    }
}