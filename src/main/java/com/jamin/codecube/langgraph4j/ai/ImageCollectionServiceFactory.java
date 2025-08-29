package com.jamin.codecube.langgraph4j.ai;

import com.jamin.codecube.langgraph4j.tools.ImageSearchTool;
import com.jamin.codecube.langgraph4j.tools.LogoGeneratorTool;
import com.jamin.codecube.langgraph4j.tools.MermaidDiagramTool;
import com.jamin.codecube.langgraph4j.tools.UndrawIllustrationTool;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 图片收集服务工厂
 */
@Slf4j
@Configuration
public class ImageCollectionServiceFactory {

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private ImageSearchTool imageSearchTool;

    @Autowired
    private UndrawIllustrationTool undrawIllustrationTool;

    @Autowired
    private MermaidDiagramTool mermaidDiagramTool;

    @Autowired
    private LogoGeneratorTool logoGeneratorTool;

    /**
     * 创建图片收集 AI 服务
     */
    @Bean
    public ImageCollectionService createImageCollectionService() {
        return AiServices.builder(ImageCollectionService.class)
                .chatModel(chatModel)
                .tools(
                        imageSearchTool,
                        undrawIllustrationTool,
                        mermaidDiagramTool,
                        logoGeneratorTool
                )
                .hallucinatedToolNameStrategy(toolExecutionRequest ->
                        ToolExecutionResultMessage.from(toolExecutionRequest,
                                "there is no tool called " + toolExecutionRequest.name()))
                .build();
    }
}