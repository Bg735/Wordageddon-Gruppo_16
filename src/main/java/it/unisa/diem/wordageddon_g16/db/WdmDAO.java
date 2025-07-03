package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.models.Document;
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
    public Optional<WDM> selectById(Object document) {
        return selectWhere("document = ?", ((Document)document).id()).stream().findFirst();
    }

    @Override
    public List<WDM> selectAll() {
        return selectBase("SELECT * FROM WDM");
    }

    public List<WDM> selectWhere(String sqlClause, Object... params) {
        String query = "SELECT * FROM WDM WHERE " + sqlClause;
        return selectBase(query, params);
    }

    private List<WDM> selectBase(String query, Object... params) {
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
        return executeQuery(query, callback, params);
    }

    @Override
    public void delete(WDM wdm) {
        String query = "DELETE FROM WDM WHERE document = ?";
        try {
            executeUpdate(query, wdm.getDocument().id());
        } catch (Exception e) {
            throw new QueryFailedException(e.getMessage());
        }
    }

    @Override
    public void update(WDM wdm) {
        String query = "UPDATE WDM SET occurrences = ? WHERE document = ? AND word = ?";
        try {
            for (Map.Entry<String, Integer> entry : wdm.getWords().entrySet()) {
                executeUpdate(query, entry.getValue(), wdm.getDocument().id(), entry.getKey());
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
                executeUpdate(query, wdm.getDocument().id(), entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            throw new QueryFailedException(e.getMessage());
        }
    }
}
