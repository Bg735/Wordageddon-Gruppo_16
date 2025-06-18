package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class DocumentDAO extends JdbcDAO<Document,Long> {
    public DocumentDAO(Connection conn) {
        super(conn);
    }

    @Override
    public Optional<Document> selectById(Long id) {
        String query = "SELECT * FROM Document WHERE id = ?";
        Callback<ResultSet,Optional<Document>> callback = res -> {
            try {
                if (res != null && res.next()) {
                    Document document = new Document(
                            res.getInt("id"),
                            res.getString("title"),
                            res.getString("path"),
                            res.getInt("wordCount")
                    );
                    return Optional.of(document);
                }
            } catch (SQLException e) {
                SystemLogger.log("Error trying to get all documents", e);
                throw new QueryFailedException(e.getMessage());
            }
            return Optional.empty();
        };

        return executeQuery(query, callback, id);
    }

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
                            res.getInt("id"),
                            res.getString("title"),
                            res.getString("path"),
                            res.getInt("wordCount")
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

    @Override
    public void insert(Document document) {
        String query = "INSERT INTO Document (title, path, wordCount) VALUES (?, ?, ?)";
        try {
            executeUpdate(query, document.getTitle(), document.getPath(), document.getWordCount());
        } catch (Exception e) {
            SystemLogger.log("Error trying to insert document: " + document, e);
            throw new QueryFailedException(e.getMessage());
        }
    }

    @Override
    public void update(Document document) {
        String query = "UPDATE Document SET title = ?, path = ?, wordCount = ? WHERE id = ?";
        try {
            executeUpdate(query, document.getTitle(), document.getPath(), document.getWordCount(), document.getId());
        } catch (Exception e) {
            SystemLogger.log("Error trying to update document: " + document, e);
            throw new UpdateFailedException(e.getMessage());
        }
    }

    @Override
    public void delete(Document document) {
        String query = "DELETE FROM Document WHERE id = ?";
        try {
            executeUpdate(query, document.getId());             // Delete on Document also deletes the associated Content and WDM due to integrity constraints (ON DELETE CASCADE)
        } catch (Exception e) {
            SystemLogger.log("Error trying to delete document: " + document, e);
            throw new UpdateFailedException(e.getMessage());
        }
    }
}
