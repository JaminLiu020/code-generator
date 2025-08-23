package com.jamin.codecube.core.handler;

import cn.hutool.core.util.StrUtil;
import com.jamin.codecube.model.entity.User;
import com.jamin.codecube.model.enums.ChatHistoryMessageTypeEnum;
import com.jamin.codecube.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 处理流式文本响应的简单处理器
 * 处理 HTML 喝 MULTI_FILE 响应流，将所有文本收集到一个字符串中
 */
@Slf4j
public class SimpleTextStreamHandler {
    /**
     * 处理流式文本响应，收集所有文本并在完成时存储到对话记录表中
     * @param originFlux
     * @param chatHistoryService
     * @param appId
     * @param loginUser
     * @return
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               Long appId, User loginUser) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return originFlux
                .map(chunk -> {
                    aiResponseBuilder.append(chunk);
                    return chunk;
                })
                //完成后存储AI消息到对话记录表
                .doOnComplete(() -> {
                    String aiResponse = aiResponseBuilder.toString();
                    if (StrUtil.isNotBlank(aiResponse)) {
                        chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    }
                })
                .doOnError(error -> {
                    // 错误记录也要存到对话记录表
                    String errorMesge = "AI回复出错：" + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMesge, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }
}
