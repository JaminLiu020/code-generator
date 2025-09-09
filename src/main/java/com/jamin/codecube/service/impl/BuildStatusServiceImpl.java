package com.jamin.codecube.service.impl;

import cn.hutool.json.JSONUtil;
import com.jamin.codecube.service.BuildStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 构建状态推送服务实现
 */
@Service
@Slf4j
public class BuildStatusServiceImpl implements BuildStatusService {
    
    /**
     * 存储应用ID对应的SSE连接
     */
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    @Override
    public SseEmitter createBuildStatusEmitter(Long appId) {
        // 创建SSE连接，设置30分钟超时
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        // 存储连接
        emitters.put(appId, emitter);
        
        // 设置连接完成时清理
        emitter.onCompletion(() -> {
            log.info("构建状态SSE连接完成，appId: {}", appId);
            emitters.remove(appId);
        });
        
        // 设置连接超时时清理
        emitter.onTimeout(() -> {
            log.info("构建状态SSE连接超时，appId: {}", appId);
            emitters.remove(appId);
        });
        
        // 设置连接错误时清理
        emitter.onError((ex) -> {
            log.error("构建状态SSE连接错误，appId: {}, error: {}", appId, ex.getMessage());
            emitters.remove(appId);
        });
        
        log.info("创建构建状态SSE连接，appId: {}", appId);
        return emitter;
    }
    
    @Override
    public void pushBuildStarted(Long appId) {
        sendEvent(appId, "build-started", "Vue项目构建已开始");
    }
    
    @Override
    public void pushBuildSuccess(Long appId) {
        sendEvent(appId, "build-success", "Vue项目构建成功");
    }
    
    @Override
    public void pushBuildFailure(Long appId, String error) {
        sendEvent(appId, "build-failure", "Vue项目构建失败: " + error);
    }
    
    /**
     * 发送SSE事件
     */
    private void sendEvent(Long appId, String eventType, String message) {
        SseEmitter emitter = emitters.get(appId);
        if (emitter != null) {
            try {
                // 构造事件数据
                Map<String, Object> eventData = Map.of(
                    "type", eventType,
                    "message", message,
                    "timestamp", System.currentTimeMillis()
                );
                
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .name(eventType)
                    .data(JSONUtil.toJsonStr(eventData));
                
                emitter.send(event);
                log.info("推送构建状态事件: appId={}, event={}, message={}", appId, eventType, message);
                
                // 如果是构建完成或失败，延迟关闭连接，让前端有时间处理事件
                if ("build-success".equals(eventType) || "build-failure".equals(eventType)) {
                    // 延迟2秒关闭，确保前端能正确处理事件
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                            emitter.complete();
                            emitters.remove(appId);
                            log.info("延迟关闭构建状态SSE连接: appId={}", appId);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
            } catch (IOException e) {
                log.error("推送构建状态事件失败: appId={}, error={}", appId, e.getMessage());
                emitters.remove(appId);
            }
        } else {
            log.debug("没有找到对应的SSE连接: appId={}", appId);
        }
    }
}
