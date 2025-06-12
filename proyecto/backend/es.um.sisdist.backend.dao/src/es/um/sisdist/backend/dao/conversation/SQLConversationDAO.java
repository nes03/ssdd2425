package es.um.sisdist.backend.dao.conversation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import es.um.sisdist.backend.dao.models.Conversation;
import es.um.sisdist.backend.dao.utils.Lazy;

public class SQLConversationDAO implements IConversationDAO {
    Supplier<Connection> conn;

    public SQLConversationDAO() {
        conn = Lazy.lazily(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();
                String sqlServerName = Optional.ofNullable(System.getenv("SQL_SERVER")).orElse("localhost");
                String dbName = Optional.ofNullable(System.getenv("DB_NAME")).orElse("ssdd");
                return DriverManager.getConnection(
                        "jdbc:mysql://" + sqlServerName + "/" + dbName + "?user=root&password=root");
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    @Override
    public void save(Conversation conversation) {
        /*
         * try {
         * PreparedStatement stm = conn.get().prepareStatement(
         * "INSERT INTO conversations (user_id, prompt, response) VALUES (?, ?, ?)");
         * stm.setString(1, conversation.getUserId());
         * stm.setString(2, conversation.getPrompt());
         * stm.setString(3, conversation.getResponse());
         * stm.executeUpdate();
         * } catch (SQLException e) {
         * e.printStackTrace();
         * }
         */

        try {
            PreparedStatement stm = conn.get().prepareStatement(
                    "INSERT INTO conversations (dialogue_id, user_id, dname, status, dialogue) VALUES (?, ?, ?, ?, ?)");
            stm.setString(1, conversation.getDialogueId());
            stm.setString(2, conversation.getUserId());
            stm.setString(3, conversation.getDname());
            stm.setString(4, conversation.getStatus()); // 'READY', 'BUSY', 'FINISHED'
            stm.setString(5, conversation.getDialogue()); // JSON como String
            stm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Conversation> findByUserId(String userId) {
        List<Conversation> list = new ArrayList<>();
        try {
            PreparedStatement stm = conn.get().prepareStatement(
                    "SELECT * FROM conversations WHERE user_id = ?");
            stm.setString(1, userId);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                list.add(createConversation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Conversation findByDialogueId(String dialogueId) {
        try {
            PreparedStatement stm = conn.get().prepareStatement(
                    "SELECT * FROM conversations WHERE dialogue_id = ?");
            stm.setString(1, dialogueId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return createConversation(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Conversation findById(int id) {
        try {
            PreparedStatement stm = conn.get().prepareStatement(
                    "SELECT * FROM conversations WHERE id = ?");
            stm.setInt(1, id);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return createConversation(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Conversation createConversation(ResultSet rs) throws SQLException {
        Conversation c = new Conversation();
        // c.setId(rs.getInt("id"));
        c.setDialogueId(rs.getString("dialogue_id"));
        c.setUserId(rs.getString("user_id"));
        c.setPrompt(rs.getString("prompt"));
        c.setResponse(rs.getString("response"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setDname(rs.getString("dname"));
        c.setStatus(rs.getString("status"));
        c.setDialogue(rs.getString("dialogue"));
        return c;
    }
}