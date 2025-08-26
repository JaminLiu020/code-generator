package com.jamin.codecube.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jamin.codecube.ai.tools.*;
import com.jamin.codecube.exception.BusinessException;
import com.jamin.codecube.exception.ErrorCode;
import com.jamin.codecube.model.enums.CodeGenTypeEnum;
import com.jamin.codecube.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private StreamingChatModel openAiStreamingChatModel;
    @Autowired
    @Qualifier("reasoningStreamingChatModel")
    private StreamingChatModel reasoningStreamingChatModel;
    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;
    @Autowired
    private ChatHistoryService chatHistoryService;

    /**
     * 使用 Caffeine 缓存来存储 AiCodeGeneratorService 实例
     * 缓存策略：
     * - 最大缓存大小：1000
     * - 写入后过期时间：30 分钟
     * - 访问后过期时间：10 分钟
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI服务实例已被移除，缓存键：{}，原因：{}", key, cause);
            })
            .build();

    /**
     * 根据应用 ID 获取 AiCodeGeneratorService
     * @param appId
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据应用 ID 和代码生成类型获取 AiCodeGeneratorService
     * @param appId
     * @param codeGenType
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 构建缓存键
     * @param appId
     * @param codeGenType
     * @return
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return codeGenType + "_" + appId;
    }

    /**
     * 创建新的 AiCodeGeneratorService 实例
     * @param appId
     * @return
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("创建新的 AI 代码生成服务实例，appId: {}", appId);
        // 根据 appId 构建独立的会话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库中加载对话记录
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        switch (codeGenType) {
            // 对于 HTML 和多文件代码生成，使用通用的 AiCodeGeneratorService
            case HTML, MULTI_FILE:{
                return AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        .build();
            }
            // 对于 Vue 项目代码生成，使用推理模型和工具调用
            case VUE_PROJECT:{
                return AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(
                                new FileWriteTool(),
                                new FileDeleteTool(),
                                new FileDirReadTool(),
                                new FileReadTool(),
                                new FileModifyTool()
                        )
                        .hallucinatedToolNameStrategy(toolExecutionRequest ->
                            ToolExecutionResultMessage.from(toolExecutionRequest,
                                    "there is no tool called " + toolExecutionRequest.name()))
                        .build();
            }
            default:
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型：" + codeGenType.getValue());
        }

    }

    /**
     * 创建默认的 AiCodeGeneratorService 实例（appId=0）
     * @return
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0);
    }
}
