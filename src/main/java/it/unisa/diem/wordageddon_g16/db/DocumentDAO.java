package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public class DocumentDAO extends JdbcDAO<Document> {
    public DocumentDAO(Connection conn) {
        super(conn);
    }

    @Override
    public Optional<Document> selectById(Object id) {
        String query = "SELECT * FROM Document WHERE id = ?";
        try (var res = executeQuery(query, id)) {
            if (res != null && res.next()) {
                Document document = new Document(
                        res.getInt("id"),
                        res.getString("title"),
                        res.getString("path"),
                        res.getInt("wordCount")
                );
                return Optional.of(document);
            }
        } catch (Exception e) {
            SystemLogger.log("Error trying to get document with id: " + id, e);
            throw new QueryFailedException(e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Document> selectAll() {
        String query = "SELECT * FROM Document";
        try (var res = executeQuery(query)) {
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
            executeUpdate(query, document.getId());
        } catch (Exception e) {
            SystemLogger.log("Error trying to delete document: " + document, e);
            throw new UpdateFailedException(e.getMessage());
        }
    }
}
