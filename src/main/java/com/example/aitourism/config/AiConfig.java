package com.example.aitourism.config;

import com.example.aitourism.ai.AssistantService;
import com.example.aitourism.ai.memory.CustomRedisChatMemoryStore;
import com.example.aitourism.ai.tool.POISearchTool;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;
import java.util.Locale;

@Configuration
public class AiConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key:}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.base-url:}")
    private String baseUrl;

        @Value("${ai.tool.mode:hybrid}")
        private String toolMode;

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
                                             Optional<POISearchTool> poiSearchToolOptional,
                                             Optional<ToolProvider> mcpToolProviderOptional) {
        var builder = AiServices.builder(AssistantService.class)
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
                                .chatMemoryProvider(chatMemoryProvider);

                String mode = toolMode == null ? "hybrid" : toolMode.toLowerCase(Locale.ROOT).trim();
                switch (mode) {
                        case "local":
                                poiSearchToolOptional.ifPresent(builder::tools);
                                break;
                        case "mcp":
                                if (mcpToolProviderOptional.isPresent()) {
                                        builder.toolProvider(mcpToolProviderOptional.get());
                                } else {
                                        System.err.println("[WARN] ai.tool.mode=mcp 但 MCP ToolProvider 未加载，已降级为无工具模式。请检查 mcp.enabled 与 mcp.url 配置，以及 AMAP_API_KEY 环境变量。");
                                }
                                break;
                        case "hybrid":
                        default:
                                poiSearchToolOptional.ifPresent(builder::tools);
                                mcpToolProviderOptional.ifPresent(builder::toolProvider);
                                break;
                }

        return builder.build();
    }
}
