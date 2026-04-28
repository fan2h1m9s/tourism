package com.example.aitourism.service;

import com.example.aitourism.ai.AssistantService;
import com.example.aitourism.dto.ChatHistoryItem;
import com.example.aitourism.dto.ChatSendRequest;
import com.example.aitourism.dto.ChatSendResponse;
import com.example.aitourism.dto.ChatSessionItem;
import com.example.aitourism.repository.ChatPersistenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemoChatService {

    private final AssistantService assistantService;
    private final ChatPersistenceRepository chatPersistenceRepository;

    @Transactional
    public ChatSendResponse send(ChatSendRequest request) {
        String sessionId = normalizeSessionId(request.getSessionId());
        String title = buildSessionTitle(request.getMessage());

        chatPersistenceRepository.createSessionIfAbsent(sessionId, request.getUserId(), title);
        chatPersistenceRepository.insertMessage(newMessageId(), sessionId, request.getUserId(), "user", request.getMessage());

        String answer = assistantService.chat(sessionId, request.getMessage());

        chatPersistenceRepository.insertMessage(newMessageId(), sessionId, request.getUserId(), "assistant", answer);
        chatPersistenceRepository.touchSession(sessionId);

        return new ChatSendResponse(sessionId, answer);
    }

    @Transactional
    public String prepareStreamSession(ChatSendRequest request) {
        String sessionId = normalizeSessionId(request.getSessionId());
        String title = buildSessionTitle(request.getMessage());

        chatPersistenceRepository.createSessionIfAbsent(sessionId, request.getUserId(), title);
        chatPersistenceRepository.insertMessage(newMessageId(), sessionId, request.getUserId(), "user", request.getMessage());

        return sessionId;
    }

    @Transactional
    public void saveStreamAnswer(String sessionId, String userId, String answer) {
        chatPersistenceRepository.insertMessage(newMessageId(), sessionId, userId, "assistant", answer);
        chatPersistenceRepository.touchSession(sessionId);
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryItem> history(String sessionId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return chatPersistenceRepository.listMessages(sessionId, safeLimit);
    }

    @Transactional(readOnly = true)
    public List<ChatSessionItem> sessions(String userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return chatPersistenceRepository.listSessions(userId, safeLimit);
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "s-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        return sessionId;
    }

    private String buildSessionTitle(String message) {
        String raw = message == null ? "新会话" : message.trim();
        if (raw.isEmpty()) {
            return "新会话";
        }
        return raw.length() > 24 ? raw.substring(0, 24) : raw;
    }

    private String newMessageId() {
        return "m-" + UUID.randomUUID().toString().replace("-", "");
    }
}
