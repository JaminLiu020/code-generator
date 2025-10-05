package com.jamin.codecube.monitor;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
@Slf4j
public class AiModelMonitorListener implements ChatModelListener {

    // 用于存储请求开始时间的键
    public static final String REQUEST_START_TIME_KEY = "request_start_time";
    // 用于存储监控上下文的键（请求和响应事件触发的线程不是同一个）
    public static final String MONITOR_CONTEXT_KEY = "monitor_context";

    @Autowired
    private AiModelMetricsCollector metricsCollector;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        // 记录请求开始时间
        requestContext.attributes().put(REQUEST_START_TIME_KEY, Instant.now());
        // 从监控上下文获取信息
        MonitorContext monitorContext = MonitorContextHolder.getContext();
        String userId = monitorContext.getUserId();
        String appId = monitorContext.getAppId();
        requestContext.attributes().put(MONITOR_CONTEXT_KEY, monitorContext);
        // 获取模型名称
        String modelName = requestContext.chatRequest().modelName();
        // 调用指标收集器记录请求指标
        metricsCollector.recordRequest(userId, appId, modelName, "started");
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        // 从监控上下文获取信息
        Map<Object, Object> attributes = responseContext.attributes();
        MonitorContext monitorContext = (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY);
        String userId = monitorContext.getUserId();
        String appId = monitorContext.getAppId();
        // 获取模型名称
        String modelName = responseContext.chatRequest().modelName();
        // 记录成功请求
        metricsCollector.recordRequest(userId, appId, modelName, "success");
        // 记录响应时间
        recordResponseTime(attributes, userId, appId, modelName);
        // 记录使用的令牌数
        recordTokenUsage(responseContext, userId, appId, modelName);
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        // 从监控上下文获取信息
        MonitorContext monitorContext = MonitorContextHolder.getContext();
        String userId = monitorContext.getUserId();
        String appId = monitorContext.getAppId();
        // 获取模型名称
        String modelName = errorContext.chatRequest().modelName();
        // 记录错误请求
        metricsCollector.recordRequest(userId, appId, modelName, "error");
        String errorMessage = errorContext.error().getMessage();
        metricsCollector.recordError(userId, appId, modelName, errorMessage);
        // 记录响应时间
        recordResponseTime(errorContext.attributes(), userId, appId, modelName);
    }

    /**
     * 记录响应时间
     * @param attributes
     * @param userId
     * @param appId
     * @param modelName
     */
    private void recordResponseTime(Map<Object, Object> attributes, String userId, String appId, String modelName) {
        Instant start = (Instant) attributes.get(REQUEST_START_TIME_KEY);
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        metricsCollector.recordResponseTime(userId, appId, modelName, duration);
    }


    /**
     * 记录使用的令牌数
     * @param responseContext
     * @param userId
     * @param appId
     * @param modelName
     */
    private void recordTokenUsage(ChatModelResponseContext responseContext, String userId, String appId, String modelName) {
        TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();
        if (tokenUsage != null) {
            metricsCollector.recordTokenUsage(userId, appId, modelName, "input", tokenUsage.inputTokenCount());
            metricsCollector.recordTokenUsage(userId, appId, modelName, "output", tokenUsage.outputTokenCount());
            metricsCollector.recordTokenUsage(userId, appId, modelName, "total", tokenUsage.totalTokenCount());
        }
    }
}
