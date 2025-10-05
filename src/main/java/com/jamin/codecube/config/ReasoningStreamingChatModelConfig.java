package com.jamin.codecube.config;

import com.jamin.codecube.monitor.AiModelMonitorListener;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.reasoning-streaming-chat-model")
@Data
public class ReasoningStreamingChatModelConfig {
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private int maxTokens;
    private double temperature;
    private boolean logRequests; // 是否记录请求
    private boolean logResponses; // 是否记录响应

    @Autowired
    private AiModelMonitorListener aiModelMonitorListener;

    /**
     * 推理流式聊天模型配置
     * @return
     */
    @Bean
    @Scope("prototype") // 每次注入都会创建一个新的实例
    public StreamingChatModel reasoningStreamingChatModelPrototype() {

        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(logRequests) // 是否记录请求
                .logResponses(logResponses) // 是否记录响应
                .timeout(Duration.ofMinutes(30))
                .listeners(List.of(aiModelMonitorListener))
                .build();
    }
}
