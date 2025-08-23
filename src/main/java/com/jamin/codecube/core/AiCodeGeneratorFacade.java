package com.jamin.codecube.core;

import cn.hutool.json.JSONUtil;
import com.jamin.codecube.ai.AiCodeGeneratorService;
import com.jamin.codecube.ai.AiCodeGeneratorServiceFactory;
import com.jamin.codecube.ai.model.HtmlCodeResult;
import com.jamin.codecube.ai.model.MultiFileCodeResult;
import com.jamin.codecube.ai.model.message.AiResponseMessage;
import com.jamin.codecube.ai.model.message.ToolExecutedMessage;
import com.jamin.codecube.ai.model.message.ToolRequestMessage;
import com.jamin.codecube.core.parse.CodeParserExecutor;
import com.jamin.codecube.core.saver.CodeFileSaverExecutor;
import com.jamin.codecube.model.enums.CodeGenTypeEnum;
import com.jamin.codecube.exception.BusinessException;
import com.jamin.codecube.exception.ErrorCode;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成器外观类
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Autowired
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;




    /**
     * 生成并保存代码文件
     * @param userMessage
     * @param codeGenTypeEnum
     * @return
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId){
        if (codeGenTypeEnum == null) {
            throw new IllegalArgumentException("Code generation type cannot be null");
        }
        // 获取 AI 代码生成服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        // 根据不同的代码生成类型调用不同的方法
        switch (codeGenTypeEnum) {
            case HTML:
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                return CodeFileSaverExecutor.executeSaver(htmlCodeResult, CodeGenTypeEnum.HTML, appId);
            case MULTI_FILE:
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                return CodeFileSaverExecutor.executeSaver(multiFileCodeResult, CodeGenTypeEnum.MULTI_FILE, appId);
            default:
                String errorMsg = "Unsupported code generation type: " + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMsg);
        }
    }

    /**
     * 生成并保存 HTML 代码文件
     * @param userMessage
     * @return
     */
    @Deprecated
    private File generateAndSaveHtmlCode(String userMessage) {
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(0L);
        HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
        return CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
    }

    /**
     * 生成并保存多文件代码
     * @param userMessage
     * @return
     */
    @Deprecated
    private File generateAndSaveMultiFileCode(String userMessage) {
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(0L);
        MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
        return CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
    }

    /**
     * 生成并保存代码流
     * @param userMessage
     * @param codeGenTypeEnum
     * @param appId
     * @return
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new IllegalArgumentException("Code generation type cannot be null");
        }
        // 获取 AI 代码生成服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        // 根据不同的代码生成类型调用不同的方法
        switch (codeGenTypeEnum) {
            case HTML:
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                return proccessCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            case MULTI_FILE:
                codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                return proccessCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            case VUE_PROJECT:
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                return proccessTokenStream(tokenStream);
            default:
                String errorMsg = "Unsupported code generation type: " + codeGenTypeEnum;
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMsg);
        }
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     * @param tokenStream
     * @return
     */
    private Flux<String> proccessTokenStream(TokenStream tokenStream) {
        //  将 TokenStream 转换为 Flux<String>
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        // 将响应转换为 AiResponseMessage
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        // 将消息转换为 JSON 字符串并发送到 Flux
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }

    /**
     * 处理代码流并保存
     * @param codeStream
     * @param codeGenTypeEnum
     * @param appId
     * @return
     */
    private Flux<String> proccessCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        // 由于是流式处理，使用StringBuilder累积结果
        StringBuilder stringBuilder = new StringBuilder();
        return codeStream
                // 累积流式结果
                .doOnNext(chunk -> {
                    stringBuilder.append(chunk);
                })
                // 流式处理完成后，进行解析和保存
                .doOnComplete(() -> {
                    try {
                        String codeContent = stringBuilder.toString();
                        Object codeResult = CodeParserExecutor.executeParser(codeContent, codeGenTypeEnum);
                        File saveDir = CodeFileSaverExecutor.executeSaver(codeResult, codeGenTypeEnum, appId);
                        log.info("保存成功，路径为：{}", saveDir.getAbsolutePath());
                    }
                    catch (Exception e) {
                        log.error("保存文件失败:{}", e.getMessage());
                    }
                });
    }

    /**
     * 生成并保存HTML代码流
     * @param userMessage
     * @return
     */
    @Deprecated
    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(0L);
        Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
        // 由于是流式处理，使用StringBuilder累积结果
        StringBuilder stringBuilder = new StringBuilder();
        return result
                // 累积流式结果
                .doOnNext(chunk -> {
                    stringBuilder.append(chunk);
                })
                // 流式处理完成后，进行解析和保存
                .doOnComplete(() -> {
                    try {
                        String codeContent = stringBuilder.toString();
                        HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(codeContent);
                        File saveDir = CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
                        log.info("保存成功，路径为：{}", saveDir.getAbsolutePath());
                    }
                    catch (Exception e) {
                        log.error("保存文件失败:{}", e.getMessage());
                    }
                });

    }

    /**
     * 生成并保存多文件代码流
     * @param userMessage
     * @return
     */
    @Deprecated
    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage){
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(0L);
        Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);

        StringBuilder stringBuilder = new StringBuilder();
        return result
                // 累积流式结果
                .doOnNext(chunk -> {
                    stringBuilder.append(chunk);
                })
                // 流式处理完成后，进行解析和保存
                .doOnComplete(() -> {
                    try {
                        String codeContent = stringBuilder.toString();
                        MultiFileCodeResult multiFileCodeResult = CodeParser.parseMultiFileCode(codeContent);
                        File saveDir = CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
                        log.info("保存成功，路径为：{}", saveDir.getAbsolutePath());
                    }
                    catch (Exception e) {
                        log.error("保存文件失败:{}", e.getMessage());
                    }
                });
    }

}
