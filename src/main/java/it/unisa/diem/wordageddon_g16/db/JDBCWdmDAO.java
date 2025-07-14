package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.db.contracts.DocumentDAO;
import it.unisa.diem.wordageddon_g16.db.contracts.WdmDAO;
import it.unisa.diem.wordageddon_g16.db.exceptions.QueryFailedException;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.WDM;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementazione JDBC del {@link WdmDAO}, che gestisce le operazioni sulla matrice parola-documento (WDM).
 * <p>
 * Le informazioni sono salvate nella tabella {@code WDM}, dove ogni riga rappresenta una parola contenuta in un documento
 * e la sua frequenza (numero di occorrenze). Il DAO collega ciascuna entry al relativo {@link Document}.
 */
public class JDBCWdmDAO extends JdbcDAO<WDM> implements WdmDAO {

    /**
     * DAO utilizzato per recuperare i documenti associati alle istanze di WDM.
     */
    private final DocumentDAO documentDAO;


    /**
     * Costruisce un nuovo {@code JDBCWdmDAO} utilizzando la connessione e il DAO dei documenti specificati.
     *
     * @param conn la connessione al database da utilizzare per le operazioni
     * @param documentDAO il DAO per la gestione dei documenti
     */
    public JDBCWdmDAO(Connection conn, DocumentDAO documentDAO) {
        super(conn);
        this.documentDAO = documentDAO;
    }

    /**
     * Recupera una singola istanza di {@link WDM} in base al documento fornito.
     *
     * @param document il documento di cui recuperare la mappa parola-frequenza
     * @return un {@code Optional} contenente l'oggetto WDM, oppure vuoto se non esiste
     */
    @Override
    public Optional<WDM> selectBy(Document document) {
        return selectWhere("document = ?", (document).filename()).stream().findFirst();
    }


    /**
     * Recupera tutte le istanze della matrice WDM presenti nel database.
     * <p>
     * Ogni WDM rappresenta un documento e la mappa delle parole contenute al suo interno con la relativa frequenza.
     *
     * @return una lista di tutte le WDM disponibili
     */
    @Override
    public List<WDM> selectAll() {
        return selectBase("SELECT * FROM WDM");
    }

    /**
     * Recupera le istanze di WDM che soddisfano una specifica clausola SQL.
     *
     * @param sqlClause la clausola WHERE da applicare (senza includere la parola chiave {@code WHERE})
     * @param params i parametri da sostituire nella query
     * @return una lista di WDM corrispondenti ai criteri forniti
     */
    public List<WDM> selectWhere(String sqlClause, Object... params) {
        String query = "SELECT * FROM WDM WHERE " + sqlClause;
        return selectBase(query, params);
    }

    /**
     * Metodo interno di utilità per eseguire una query e convertire i risultati in oggetti {@link WDM}.
     * <p>
     * Costruisce dinamicamente le istanze WDM aggregando le parole e le frequenze associate a ciascun documento.
     *
     * @param query la query SQL da eseguire
     * @param params i parametri da sostituire nella query
     * @return una lista di oggetti WDM ottenuti dai risultati della query
     * @throws QueryFailedException se si verifica un errore durante l'elaborazione
     */
    private List<WDM> selectBase(String query, Object... params) {
        Callback<ResultSet, List<WDM>> callback = res -> {
            try {
                if (res == null) {
                    return List.of();
                }
                Map<String, WDM> wdmMap = new HashMap<>();
                while (res.next()) {
                    String filename = res.getString("document");
                    var document = documentDAO.selectBy(filename);
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
     * Elimina tutte le parole associate a un documento dalla tabella WDM.
     *
     * @param wdm la matrice parola-documento da eliminare
     * @throws QueryFailedException se si verifica un errore durante la cancellazione
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
     * Aggiorna le frequenze delle parole per un documento nella tabella WDM.
     * <p>
     * Per ogni parola nella mappa associata al documento, aggiorna la relativa frequenza.
     *
     * @param wdm la matrice parola-documento contenente i nuovi valori
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
     * Inserisce una nuova matrice parola-documento nella tabella WDM.
     * <p>
     * Per ogni parola nel documento, viene inserita una riga con il numero di occorrenze.
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