package com.jamin.codecube.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jamin.codecube.ai.model.message.*;
import com.jamin.codecube.ai.tools.BaseTool;
import com.jamin.codecube.ai.tools.ToolManager;
import com.jamin.codecube.model.entity.ChatHistoryOriginal;
import com.jamin.codecube.model.entity.User;
import com.jamin.codecube.model.enums.ChatHistoryMessageTypeEnum;
import com.jamin.codecube.service.ChatHistoryOriginalService;
import com.jamin.codecube.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 处理 JSON 消息流的组件
 * 处理 VUR_PROJECT 类型的复杂流式响应，包含工具调用信息
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {
    @Autowired
    private ToolManager toolManager;

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
                               ChatHistoryOriginalService chatHistoryOriginalService,
                               Long appId, User loginUser) {
        // 收集数据用于前端展示
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 收集用于恢复对话记忆的数据
        StringBuilder aiResponseStringBuilder = new StringBuilder();
        // 每个 Flux 流可能包含多条工具调用和 AI_RESPONSE 响应信息，统一收集之后批量入库
        List<ChatHistoryOriginal> originalChatHistoryList = new ArrayList<>();
        // 跟踪已见过的工具ID，判断是否是第一次调用
        Set<String> seenToolId = new HashSet<>();
        return originFlux
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, aiResponseStringBuilder, originalChatHistoryList, seenToolId);
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字符串
                //完成后存储AI消息到对话记录表
                .doOnComplete(() -> {
                    // 工具调用信息入库
                    if (!originalChatHistoryList.isEmpty()) {
                        // 完善 ChatHistoryOriginal 信息
                        originalChatHistoryList.forEach(chatHistory -> {
                            chatHistory.setAppId(appId);
                            chatHistory.setUserId(loginUser.getId());
                        });
                        // 批量入库
                        chatHistoryOriginalService.addOriginalChatMessageBatch(originalChatHistoryList);
                    }
                    // Ai response 入库(两种情况：1. 没有进行工具调用。2. 工具调用结束之后 AI 一般还会有一句返回)
                    String aiResponseStr = aiResponseStringBuilder.toString();
                    chatHistoryOriginalService.addOriginalChatMessage(appId, aiResponseStr, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());

                    // 将收集到的聊天记录存储到对话记录表
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                })
                .doOnError(error -> {
                    // 错误记录也要存到对话记录表
                    String errorMessage = "AI回复出错：" + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    chatHistoryOriginalService.addOriginalChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, StringBuilder aiResponseStringBuilder, List<ChatHistoryOriginal> originalChatHistoryList, Set<String> seenToolId) {
        // 解析JSON，获取消息类型
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        // 根据消息类型选择不同的处理逻辑
        switch (typeEnum) {
            case AI_RESPONSE -> {
                // AI 回复消息
                AiResponseMessage aiResponseMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiResponseMessage.getData();
                // 直接拼接响应
                chatHistoryStringBuilder.append(data);
                // 对于 AI 响应内容，与展示数据处理逻辑相同
                aiResponseStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                // 工具请求消息
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                // 检查工具 ID 是否重复
                String toolId = toolRequestMessage.getId();
                if (toolId != null && !seenToolId.contains(toolId)) {
                    // 第一次调用这个工具，记录ID并返回信息
                    seenToolId.add(toolId);
                    // 获取工具名称与工具实例
                    String toolName = toolRequestMessage.getName();
                    BaseTool tool = toolManager.getTool(toolName);
                    return tool.generateToolRequestResponse();
                }
                else {
                    // 重复的工具请求，忽略
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                // 处理工具调用信息
                processToolExecutionMessage(aiResponseStringBuilder, chunk, originalChatHistoryList);

                // 工具执行结果消息
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                // 获取工具参数
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                // 获取工具
                String toolName = toolExecutedMessage.getName();
                BaseTool tool = toolManager.getTool(toolName);
                // 生成工具执行结果
                String result = tool.generateToolExecutedResult(jsonObject);
                // 将工具执行结果写入聊天记录并返回前端
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

    /**
     * 解析处理工具调用相关信息
     * @param aiResponseStringBuilder
     * @param chunk
     * @param originalChatHistoryList
     */
    private void processToolExecutionMessage(StringBuilder aiResponseStringBuilder, String chunk, List<ChatHistoryOriginal> originalChatHistoryList) {
        // 解析 chunk
        ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
        // 构造工具调用请求对象(工具调用结果的数据就是从调用请求里拿的，所以直接在这里处理调用请求信息)
        String aiResponseStr = aiResponseStringBuilder.toString();
        ToolRequestMessage toolRequestMessage = new ToolRequestMessage();
        toolRequestMessage.setId(toolExecutedMessage.getId());
        toolRequestMessage.setName(toolExecutedMessage.getName());
        toolRequestMessage.setArguments(toolExecutedMessage.getArguments());
        toolRequestMessage.setText(aiResponseStr);
        // 转换成 JSON
        String toolRequestJsonStr = JSONUtil.toJsonStr(toolRequestMessage);
        // 构造 ChatHistory 存入列表
        ChatHistoryOriginal toolRequestHistory = ChatHistoryOriginal.builder()
                .message(toolRequestJsonStr)
                .messageType(ChatHistoryMessageTypeEnum.TOOL_EXECUTION_REQUEST.getValue())
                .build();
        originalChatHistoryList.add(toolRequestHistory);
        ChatHistoryOriginal toolResultHistory = ChatHistoryOriginal.builder()
                .message(chunk)
                .messageType(ChatHistoryMessageTypeEnum.TOOL_EXECUTION_RESULT.getValue())
                .build();
        originalChatHistoryList.add(toolResultHistory);
        // AI 响应内容暂时结束，置空 aiResponseStringBuilder
        aiResponseStringBuilder.setLength(0);
    }
}
