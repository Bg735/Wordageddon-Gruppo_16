package it.unisa.diem.wordageddon_g16.db;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StopWordDAO extends JdbcDAO<String> {
    public StopWordDAO(Connection conn) {
        super(conn);
    }

    @Override
    public Optional<String> selectById(Object oid) {
        throw new UnsupportedOperationException("This operation is not implemented as it is not useful in this context.");
    }

    @Override
    public List<String> selectAll() {
        String query = "SELECT word FROM StopWord";
        Callback<ResultSet,List<String>> callback = res -> {
            try{
                List<String> stopWords = new ArrayList<>();
                while (res.next()) {
                    stopWords.add(res.getString("word"));
                }
                return stopWords;
            } catch (SQLException e) {
                SystemLogger.log("Error trying to get all stop words", e);
                throw new QueryFailedException(e.getMessage());
            }
        };
        return executeQuery(query, callback);

    }

    @Override
    public void insert(String s) {
        String query = "INSERT INTO StopWord (word) VALUES (?)";
        try {
            executeUpdate(query, s);
        } catch (SQLException e) {
            SystemLogger.log("Error trying to insert stop word: " + s, e);
            throw new QueryFailedException(e.getMessage());
        }
    }

    @Override
    public void update(String s) {
        throw new UnsupportedOperationException("This operation is not implemented as it cannot be used in this context.");
    }

    @Override
    public void delete(String s) {
        String query = "DELETE FROM StopWord WHERE word = ?";
        try {
            executeUpdate(query, s);
        } catch (SQLException e) {
            SystemLogger.log("Error trying to delete stop word: " + s, e);
            throw new QueryFailedException(e.getMessage());
        }
    }
}
