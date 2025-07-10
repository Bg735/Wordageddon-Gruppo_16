package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.db.exceptions.QueryFailedException;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.WDM;
import javafx.util.Callback;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object (DAO) per la gestione della matrice parola-documento (WDM).
 * Permette di eseguire operazioni CRUD sulla tabella WDM del database,
 * associando ogni documento alle sue parole e relative frequenze.
 */
public class WdmDAO extends JdbcDAO<WDM> {

    /**
     * DAO utilizzato per recuperare i documenti associati alle istanze di WDM.
     */
    private final DAO<Document> documentDAO;

    /**
     * Costruisce un nuovo WdmDAO utilizzando la connessione e il DAO dei documenti specificati.
     *
     * @param conn la connessione al database da utilizzare per le operazioni
     * @param documentDAO il DAO per la gestione dei documenti
     */
    public WdmDAO(Connection conn, DAO<Document> documentDAO) {
        super(conn);
        this.documentDAO = documentDAO;
    }

    /**
     * Recupera una singola istanza di WDM in base al documento fornito.
     *
     * @param document il documento di cui recuperare la matrice parola-frequenza
     * @return un Optional contenente la WDM trovata, o vuoto se non esiste
     */
    @Override
    public Optional<WDM> selectById(Object document) {
        return selectWhere("document = ?", ((Document)document).filename()).stream().findFirst();
    }

    /**
     * Recupera tutte le istanze di WDM presenti nella tabella.
     *
     * @return una lista di tutte le WDM nel database
     */
    @Override
    public List<WDM> selectAll() {
        return selectBase("SELECT * FROM WDM");
    }

    /**
     * Recupera tutte le istanze di WDM che soddisfano una specifica clausola SQL.
     *
     * @param sqlClause la clausola WHERE da applicare (senza la parola chiave WHERE)
     * @param params i parametri da sostituire nella query
     * @return una lista di WDM corrispondenti alla clausola specificata
     */
    public List<WDM> selectWhere(String sqlClause, Object... params) {
        String query = "SELECT * FROM WDM WHERE " + sqlClause;
        return selectBase(query, params);
    }

    /**
     * Metodo di utilità per eseguire una query e mappare i risultati in oggetti WDM.
     *
     * @param query la query SQL da eseguire
     * @param params i parametri da sostituire nella query
     * @return una lista di WDM ottenute dai risultati della query
     */
    private List<WDM> selectBase(String query, Object... params) {
        // FA CRASHARE IL THREAD PRINCIPALE
        Callback<ResultSet, List<WDM>> callback = res -> {
            try {
                if (res == null) {
                    return List.of();
                }
                Map<String, WDM> wdmMap = new HashMap<>();
                while (res.next()) {
                    String filename = res.getString("document");
                    var document = documentDAO.selectById(filename);
                    if (document.isPresent()) {
                        WDM wdm = wdmMap.computeIfAbsent(filename, k -> new WDM(document.get(), new HashMap<>()));
                        wdm.getWords().put(res.getString("word"), res.getInt("occurrences"));
                    }
                }
                return List.copyOf(wdmMap.values());
            } catch (Exception e) {
                throw new QueryFailedException(e.getMessage());
            }
        };
        return executeQuery(query, callback, params);
    }

    /**
     * Elimina tutte le occorrenze di parole associate a un documento dalla tabella WDM.
     *
     * @param wdm la matrice parola-documento da eliminare
     * @throws QueryFailedException se si verifica un errore durante l'eliminazione
     */
    @Override
    public void delete(WDM wdm) {
        String query = "DELETE FROM WDM WHERE document = ?";
        try {
            executeUpdate(query, wdm.getDocument().filename());
        } catch (Exception e) {
            throw new QueryFailedException(e.getMessage());
        }
    }

    /**
     * Aggiorna le frequenze delle parole associate a un documento nella tabella WDM.
     *
     * @param wdm la matrice parola-documento da aggiornare
     * @throws QueryFailedException se si verifica un errore durante l'aggiornamento
     */
    @Override
    public void update(WDM wdm) {
        String query = "UPDATE WDM SET occurrences = ? WHERE document = ? AND word = ?";
        try {
            for (Map.Entry<String, Integer> entry : wdm.getWords().entrySet()) {
                executeUpdate(query, entry.getValue(), wdm.getDocument().filename(), entry.getKey());
            }
        } catch (Exception e) {
            throw new QueryFailedException(e.getMessage());
        }
    }

    /**
     * Inserisce tutte le parole e relative frequenze di un documento nella tabella WDM.
     *
     * @param wdm la matrice parola-documento da inserire
     * @throws QueryFailedException se si verifica un errore durante l'inserimento
     */
    @Override
    public void insert(WDM wdm) {
        String query = "INSERT INTO WDM (document, word, occurrences) VALUES (?, ?, ?)";
        try{
            for (Map.Entry<String, Integer> entry : wdm.getWords().entrySet()) {
                executeUpdate(query, wdm.getDocument().filename(), entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            throw new QueryFailedException(e.getMessage());
        }
    }
}