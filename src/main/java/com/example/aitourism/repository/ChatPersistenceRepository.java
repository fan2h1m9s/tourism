package com.example.aitourism.repository;

import com.example.aitourism.dto.ChatHistoryItem;
import com.example.aitourism.dto.ChatSessionItem;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatPersistenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public void createSessionIfAbsent(String sessionId, String userId, String title) {
        jdbcTemplate.update(
                """
                INSERT INTO t_ai_session(session_id, user_id, title)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE modify_time = CURRENT_TIMESTAMP
                """,
                sessionId, userId, title
        );
    }

    public void touchSession(String sessionId) {
        jdbcTemplate.update(
                """
                UPDATE t_ai_session
                SET last_message_time = CURRENT_TIMESTAMP,
                    modify_time = CURRENT_TIMESTAMP
                WHERE session_id = ?
                """,
                sessionId
        );
    }

    public void insertMessage(String msgId, String sessionId, String userId, String role, String content) {
        jdbcTemplate.update(
                """
                INSERT INTO t_ai_message(msg_id, session_id, user_id, role, content)
                VALUES (?, ?, ?, ?, ?)
                """,
                msgId, sessionId, userId, role, content
        );
    }

    public List<ChatHistoryItem> listMessages(String sessionId, int limit) {
        return jdbcTemplate.query(
                """
                SELECT msg_id, role, content, created_time
                FROM t_ai_message
                WHERE session_id = ?
                ORDER BY created_time ASC
                LIMIT ?
                """,
                (rs, rowNum) -> new ChatHistoryItem(
                        rs.getString("msg_id"),
                        rs.getString("role"),
                        rs.getString("content"),
                        rs.getTimestamp("created_time").toLocalDateTime()
                ),
                sessionId,
                limit
        );
    }

    public List<ChatSessionItem> listSessions(String userId, int limit) {
        return jdbcTemplate.query(
                """
                SELECT session_id, title, last_message_time
                FROM t_ai_session
                WHERE user_id = ?
                ORDER BY last_message_time DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new ChatSessionItem(
                        rs.getString("session_id"),
                        rs.getString("title"),
                        rs.getTimestamp("last_message_time").toLocalDateTime()
                ),
                userId,
                limit
        );
    }
}
