package com.jamin.codecube.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** * AI 代码生成服务工厂类
 * 用于创建 AiCodeGeneratorService 的实例
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Autowired
    private ChatModel chatModel;
    @Autowired
    private StreamingChatModel streamingChatModel;
    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * 使用 Caffeine 缓存来存储 AiCodeGeneratorService 实例
     * 缓存策略：
     * - 最大缓存大小：1000
     * - 写入后过期时间：30 分钟
     * - 访问后过期时间：10 分钟
     */
    private final Cache<Long, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI服务实例已被移除，appID：{}，原因：{}", key, cause);
            })
            .build();

    /**
     * 根据应用 ID 获取 AiCodeGeneratorService
     * @param appId
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return serviceCache.get(appId, this::createAiCodeGeneratorService);
    }

    /**
     * 创建新的 AiCodeGeneratorService 实例
     * @param appId
     * @return
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId) {
        log.info("创建新的 AI 代码生成服务实例，appId: {}", appId);
        // 根据 appId 构建独立的会话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .build();

    }

    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();
    }
}
