package com.example.aitourism.controller;

import com.example.aitourism.ai.AssistantService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 允许前端跨域
public class ChatController {

    private final AssistantService assistantService;

    @PostMapping("/send")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = assistantService.chat(request.getSessionId(), request.getMessage());
        return new ChatResponse(answer);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        return assistantService.chatStream(request.getSessionId(), request.getMessage());
    }

    @Data
    public static class ChatRequest {
        private String sessionId;
        private String message;
    }

    @Data
    public static class ChatResponse {
        private String answer;
        public ChatResponse(String answer) {
            this.answer = answer;
        }
    }
}
