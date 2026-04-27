package com.example.aitourism.config;

import com.example.aitourism.ai.AssistantService;
import com.example.aitourism.ai.memory.CustomRedisChatMemoryStore;
import com.example.aitourism.ai.tool.POISearchTool;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class AiConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key:}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.base-url:}")
    private String baseUrl;

    @Bean
    public ChatMemoryProvider chatMemoryProvider(CustomRedisChatMemoryStore customRedisChatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .chatMemoryStore(customRedisChatMemoryStore)
                .build();
    }

    @Bean
    public AssistantService assistantService(ChatMemoryProvider chatMemoryProvider,
                                             POISearchTool poiSearchTool) {
        return AiServices.builder(AssistantService.class)
                .chatModel(OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName("deepseek-chat")
                        .build())
                .streamingChatModel(OpenAiStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName("deepseek-chat")
                        .build())
                .chatMemoryProvider(chatMemoryProvider)
                .tools(poiSearchTool) 
                .build();
    }
}
