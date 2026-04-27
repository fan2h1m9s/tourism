package com.example.aitourism.ai;

import dev.langchain4j.service.MemoryId;
import reactor.core.publisher.Flux;

public interface AssistantService {

    @dev.langchain4j.service.SystemMessage(fromResource = "prompt/tour-route-planning-system-prompt.txt")
    Flux<String> chatStream(@MemoryId String memoryId, @dev.langchain4j.service.UserMessage String userMessage);

    @dev.langchain4j.service.SystemMessage(fromResource = "prompt/tour-route-planning-system-prompt.txt")
    String chat(@MemoryId String memoryId, @dev.langchain4j.service.UserMessage String userMessage);
}
