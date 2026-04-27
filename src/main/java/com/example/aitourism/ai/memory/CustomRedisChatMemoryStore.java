package com.example.aitourism.ai.memory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.ArrayList;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
@Primary
public class CustomRedisChatMemoryStore implements ChatMemoryStore {
    
    private final StringRedisTemplate stringRedisTemplate;
    
    @Value("${ai.memory.redis.key-prefix:ai:memory:}")
    private String keyPrefix;
    
    @Value("${ai.memory.redis.ttl:1800}")
    private long ttlSeconds;

    private String buildKey(Object memoryId) {
        return keyPrefix + memoryId;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        try {
            long start = System.currentTimeMillis();
            String json = stringRedisTemplate.opsForValue().get(buildKey(memoryId));
            List<ChatMessage> messages = json != null ? messagesFromJson(json) : new ArrayList<>();
            long cost = System.currentTimeMillis() - start;
            if (cost > 100) {
               log.warn("Redis 读取延迟较高: {} ms", cost); // 匹配简历中的 "会话读写P99延迟小于100ms"
            }
            return messages;
        } catch (Exception e) {
            log.warn("读取Redis记忆失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        try {
            long start = System.currentTimeMillis();
            String json = messagesToJson(messages == null ? new ArrayList<>() : messages);
            stringRedisTemplate.opsForValue().set(buildKey(memoryId), json, Duration.ofSeconds(ttlSeconds));
            long cost = System.currentTimeMillis() - start;
            if (cost > 100) {
               log.warn("Redis 写入延迟较高: {} ms", cost); 
            }
        } catch (Exception e) {
            log.warn("写入Redis记忆失败: {}", e.getMessage());
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        try {
            stringRedisTemplate.delete(buildKey(memoryId));
        } catch (Exception e) {
            log.warn("删除Redis记忆失败: {}", e.getMessage());
        }
    }
}
