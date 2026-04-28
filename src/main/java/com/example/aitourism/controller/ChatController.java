package com.example.aitourism.controller;

import com.example.aitourism.ai.AssistantService;
import com.example.aitourism.dto.ChatHistoryItem;
import com.example.aitourism.dto.ChatSendRequest;
import com.example.aitourism.dto.ChatSendResponse;
import com.example.aitourism.dto.ChatSessionItem;
import com.example.aitourism.service.DemoChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Validated
public class ChatController {

    private final DemoChatService demoChatService;
    private final AssistantService assistantService;

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @PostMapping("/send")
    public ChatSendResponse chat(@Valid @RequestBody ChatSendRequest request) {
        return demoChatService.send(request);
    }

    @GetMapping("/history")
    public List<ChatHistoryItem> history(@RequestParam String sessionId,
                                         @RequestParam(defaultValue = "50") int limit) {
        return demoChatService.history(sessionId, limit);
    }

    @GetMapping("/sessions")
    public List<ChatSessionItem> sessions(@RequestParam String userId,
                                          @RequestParam(defaultValue = "50") int limit) {
        return demoChatService.sessions(userId, limit);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> chatStream(@Valid @RequestBody ChatSendRequest request) {
        String sessionId = demoChatService.prepareStreamSession(request);
        String userId = request.getUserId();
        AtomicReference<String> fullAnswer = new AtomicReference<>("");

        Flux<String> body = assistantService.chatStream(sessionId, request.getMessage())
                .doOnNext(token -> fullAnswer.updateAndGet(s -> s + token))
                .doOnComplete(() -> {
                    String answer = fullAnswer.get();
                    if (!answer.isBlank()) {
                        demoChatService.saveStreamAnswer(sessionId, userId, answer);
                    }
                })
                .doOnError(e -> log.error("流式响应异常: sessionId={}", sessionId, e));

        return ResponseEntity.ok()
                .header("X-Session-Id", sessionId)
                .body(body);
    }
}
