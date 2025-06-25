package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.JdbcRepository;
import it.unisa.diem.wordageddon_g16.models.WDM;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WdmDAO extends JdbcDAO<WDM> {

    private final DAO<Document> documentDAO;

    public WdmDAO(Connection conn, DAO<Document> documentDAO) {
        super(conn);
        this.documentDAO = documentDAO;
    }

    @Override
    public Optional<WDM> selectById(Object oid) {
        Long id = (Long) oid;
        String query = "SELECT * FROM WDM WHERE document_id = ?";
        Callback<ResultSet, Optional<WDM>> callback = res -> {
            try {
                if (res != null && res.next()) {
                    var document = documentDAO.selectById(id);
                    if (document.isPresent()) {
                        Map<String, Integer> wordCount = new HashMap<>();
                        while (res.next()) {
                            wordCount.put(res.getString("word"), res.getInt("occurrences"));
                        }
                        WDM wdm = new WDM(
                            document.get(),
                            wordCount
                        );
                        return Optional.of(wdm);
                    }
                }
            } catch (Exception e) {
                throw new QueryFailedException(e.getMessage());
            }
            return Optional.empty();
        };
        return executeQuery(query, callback, id);

    }

    @Override
    public List<WDM> selectAll() {
        String query = "SELECT * FROM WDM";
        Callback<ResultSet, List<WDM>> callback = res -> {
            try {
                if (res == null) {
                    return List.of();
                }
                Map<Long, WDM> wdmMap = new HashMap<>();
                while (res.next()) {
                    Long docId = res.getLong("document");
                    var document = documentDAO.selectById(docId);
                    if (document.isPresent()) {
                        WDM wdm = wdmMap.computeIfAbsent(docId, k -> new WDM(document.get(), new HashMap<>()));
                        wdm.getWords().put(res.getString("word"), res.getInt("occurrences"));
                    }
                }
                return List.copyOf(wdmMap.values());
            } catch (Exception e) {
                throw new QueryFailedException(e.getMessage());
            }
        };
        return executeQuery(query, callback);
    }

    @Override
    public void delete(WDM wdm) {
        String query = "DELETE FROM WDM WHERE document_id = ?";
        try {
            executeUpdate(query, wdm.getDocument().getId());
        } catch (Exception e) {
            throw new QueryFailedException(e.getMessage());
        }
    }

    @Override
    public void update(WDM wdm) {
        String query = "UPDATE WDM SET occurrences = ? WHERE document_id = ? AND word = ?";
        try {
            for (Map.Entry<String, Integer> entry : wdm.getWords().entrySet()) {
                executeUpdate(query, entry.getValue(), wdm.getDocument().getId(), entry.getKey());
            }
        } catch (Exception e) {
            throw new QueryFailedException(e.getMessage());
        }
    }

    @Override
    public void insert(WDM wdm) {
        String query = "INSERT INTO WDM (document, word, occurrences) VALUES (?, ?, ?)";
        try{
            for (Map.Entry<String, Integer> entry : wdm.getWords().entrySet()) {
                executeUpdate(query, wdm.getDocument().getId(), entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            throw new QueryFailedException(e.getMessage());
        }
    }
}
