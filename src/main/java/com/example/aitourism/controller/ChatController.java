package com.example.aitourism.controller;

import com.example.aitourism.dto.ChatHistoryItem;
import com.example.aitourism.dto.ChatSendRequest;
import com.example.aitourism.dto.ChatSendResponse;
import com.example.aitourism.dto.ChatSessionItem;
import com.example.aitourism.service.DemoChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 允许前端跨域
@Validated
public class ChatController {

    private final DemoChatService demoChatService;

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
    public Flux<String> chatStream(@Valid @RequestBody ChatSendRequest request) {
        ChatSendResponse response = demoChatService.send(request);
        return Flux.just(response.getAnswer());
    }
}
