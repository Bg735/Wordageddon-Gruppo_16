package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.models.*;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GameReportDAO extends JdbcDAO<GameReport> {

    private final UserDAO userDAO;
    private final DocumentDAO documentDAO;

    public GameReportDAO(Connection conn, DAO<Document> documentDAO, DAO<User> userDAO) {
        super(conn);
        this.userDAO = (UserDAO) userDAO;
        this.documentDAO = (DocumentDAO) documentDAO;
    }

    @Override
    public Optional<GameReport> selectById(Object oid) {
        Long id = (Long) oid;
        return selectBase("SELECT * FROM GameReport WHERE id = ?", id).stream().findFirst();
    }

    @Override
    public List<GameReport> selectAll() {
        return selectBase("SELECT * FROM GameReport");
    }

    public List<GameReport> selectWhere(String sqlClause, Object... params) {
        String query = "SELECT * FROM GameReport WHERE " + sqlClause;
        return selectBase(query, params);
    }

    private List<GameReport> selectBase(String query, Object... params) {
        var result = new ArrayList<GameReport>();
        Callback<ResultSet,List<GameReport>> callback = res -> {
            try {
                if (res == null) {
                    return result;
                }
                while (res.next()) {
                    var user = userDAO.selectById(res.getString("user"));
                    var docList = executeQuery(
                            "SELECT document FROM Content WHERE report = ?",
                            docs -> {
                                List<Document> documents = new ArrayList<>();
                                try {
                                    while (docs.next()) {
                                        documentDAO.selectById(docs.getLong("document")).ifPresent(documents::add);
                                    }
                                } catch (SQLException e) {
                                    SystemLogger.log("Error trying to get documents", e);
                                    throw new QueryFailedException(e.getMessage());
                                }
                                return documents;
                            },
                            res.getLong("id")
                    );
                    if (user.isPresent()) { //The user being present is guaranteed by the foreign key constraint in the database
                        result.add(new GameReport(
                                res.getLong("id"),
                                user.get(),
                                docList,
                                res.getTimestamp("timestamp").toLocalDateTime(),
                                Difficulty.valueOf(res.getString("difficulty")),
                                Duration.parse(res.getString("max_time")),
                                Duration.parse(res.getString("used_time")),
                                res.getInt("question_count"),
                                res.getInt("score")
                        ));
                    }
                }
            } catch (SQLException e) {
                SystemLogger.log("Error trying to get all game reports", e);
                throw new QueryFailedException(e.getMessage());
            }
            return result;
        };
        return executeQuery(query, callback, params);
    }

    @Override
    public void insert(GameReport gameReport) {
        String updateOnReport = "INSERT INTO GameReport (user, timestamp, difficulty, max_time, used_time, question_count, score) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String updateOnContent = "INSERT INTO Content (report, document) VALUES (?, ?)";
        try {
            executeUpdate(updateOnReport,
                gameReport.getUser().getName(),
                gameReport.getTimestamp(),
                gameReport.getDifficulty().name(),
                gameReport.getMaxTime().toString(),
                gameReport.getUsedTime().toString(),
                gameReport.getQuestionCount(),
                gameReport.getScore()
            );              // Insert on GameReport must be done first to ensure the foreign key constraint is satisfied
            for (Document document : gameReport.getDocuments()) {
                executeUpdate(updateOnContent, gameReport.getId(), document.getId());
            }

        } catch (SQLException e) {
            SystemLogger.log("Error trying to insert game report", e);
            throw new QueryFailedException(e.getMessage());
        }
    }

    @Override
    public void update(GameReport gameReport) {
        String update = "UPDATE GameReport SET user = ?, timestamp = ?, difficulty = ?, max_time = ?, used_time = ?, question_count = ?, score = ? WHERE id = ?";
        try {
            executeUpdate(update,
                gameReport.getUser().getName(),
                gameReport.getTimestamp(),
                gameReport.getDifficulty().name(),
                gameReport.getMaxTime().toString(),
                gameReport.getUsedTime().toString(),
                gameReport.getQuestionCount(),
                gameReport.getScore(),
                gameReport.getId()
            );
        } catch (SQLException e) {
            SystemLogger.log("Error trying to update game report with id: " + gameReport.getId(), e);
            throw new UpdateFailedException(e.getMessage());
        }
    }

    @Override
    public void delete(GameReport gameReport) {
        String updateOnReport = "DELETE FROM GameReport WHERE id = ?";
        try {
            executeUpdate(updateOnReport, gameReport.getId());              // Delete on GameReport also deletes the associated Content due to integrity constraints (ON DELETE CASCADE)
        } catch (SQLException e) {
            SystemLogger.log("Error trying to delete game report with id: " + gameReport.getId(), e);
            throw new UpdateFailedException(e.getMessage());
        }
    }

}
