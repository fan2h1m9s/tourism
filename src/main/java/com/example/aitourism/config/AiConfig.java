package com.example.aitourism.config;

import com.example.aitourism.ai.AssistantService;
import com.example.aitourism.ai.memory.CustomRedisChatMemoryStore;
import com.example.aitourism.ai.tool.POISearchTool;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatMemoryProvider chatMemoryProvider(CustomRedisChatMemoryStore customRedisChatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .chatMemoryStore(customRedisChatMemoryStore)
                .build();
    }

    /**
     * 将 Streaming 模型和 Tool (工具) 注入到 Agent 服务中。
     * 流式模型用于流式打字输出，普通模型可用于常规短文本或备用。
     * 注意目前使用的 DeepSeek 等很多国内模型已经支持 Function Calling / Tool use! 
     */
    @Bean
    public AssistantService assistantService(StreamingChatLanguageModel streamingChatLanguageModel,
                                             ChatLanguageModel chatLanguageModel,
                                             ChatMemoryProvider chatMemoryProvider,
                                             POISearchTool poiSearchTool) {
        return AiServices.builder(AssistantService.class)
                .chatLanguageModel(chatLanguageModel)
                .streamingChatLanguageModel(streamingChatLanguageModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(poiSearchTool) // 【亮点3】在这里把 POISearchTool 挂载进了大模型中，使大模型获得外部眼睛和双手
                .build();
    }
}
