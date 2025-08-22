package com.jamin.codecube.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
//@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
@Data
public class ReasoningStreamingChatModelConfig {
//    private String baseUrl;
//    private String apiKey;

    /**
     * 推理流式聊天模型配置
     * @return
     */
    @Bean(name = "reasoningStreamingChatModel")
    public StreamingChatModel reasoningStreamingChatModel() {
        final String baseUrl = "https://api.siliconflow.cn/";
        final String apiKey = "sk-manljlljcmxgwvvjulrcfvlmyxkidmactxczihbegoxmimrk";
        final int maxTokens = 131072; // 最大token数 128K

        // 测试环境使用
        final String modelName = "Qwen/Qwen3-Coder-30B-A3B-Instruct";

        // 生产环境使用
//        final String modelName = "Qwen/Qwen3-30B-A3B-Thinking-2507";

        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .logRequests(true) // 是否记录请求
                .logResponses(true) // 是否记录响应
                .build();
    }
}
