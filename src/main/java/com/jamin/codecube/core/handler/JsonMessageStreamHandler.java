package com.jamin.codecube.core.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jamin.codecube.ai.model.message.*;
import com.jamin.codecube.model.entity.User;
import com.jamin.codecube.model.enums.ChatHistoryMessageTypeEnum;
import com.jamin.codecube.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * 处理 JSON 消息流的组件
 * 处理 VUR_PROJECT 类型的复杂流式响应，包含工具调用信息
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    /**
     * 处理 TokenStream（VUE_PROJECT）
     * 解析 JSON 消息并重组为完整的响应格式
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               Long appId, User loginUser) {
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 跟踪已见过的工具ID，判断是否是第一次调用
        Set<String> seenToolId = new HashSet<>();
        return originFlux
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolId);
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字符串
                //完成后存储AI消息到对话记录表
                .doOnComplete(() -> {
                    // 将收集到的聊天记录存储到对话记录表
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                })
                .doOnError(error -> {
                    // 错误记录也要存到对话记录表
                    String errorMesge = "AI回复出错：" + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMesge, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolId) {
        // 解析JSON，获取消息类型
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        // 根据消息类型选择不同的处理逻辑
        switch (typeEnum) {
            case AI_RESPONSE -> {
                // AI 回复消息
                AiResponseMessage aiResponseMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiResponseMessage.getData();
                chatHistoryStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                // 工具请求消息
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                // 检查工具 ID 是否重复
                String toolId = toolRequestMessage.getId();
                if (toolId != null && !seenToolId.contains(toolId)) {
                    seenToolId.add(toolId);
                    return "\n\n[🔧选择工具] 写入文件\n\n";
                }
                else {
                    // 重复的工具请求，忽略
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                // 工具执行结果消息
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                String relativeFilePath = jsonObject.getStr("relativeFilePath");
                String suffix = FileUtil.getSuffix(relativeFilePath); // 获取文件后缀
                String content = jsonObject.getStr("content");
                String result = String.format("""
                        [✔工具调用] 写入文件 %s
                        ```%s
                        %s
                        ```
                        """, relativeFilePath, suffix, content);
                // 将工具执行结果写入聊天记录
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            }
            default -> {
                log.warn("未知的消息类型: {}", typeEnum);
                return "";
            }
        }
    }
}
