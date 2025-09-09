package com.jamin.codecube.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 构建状态推送服务
 */
public interface BuildStatusService {
    
    /**
     * 创建SSE连接来监听构建状态
     *
     * @param appId 应用ID
     * @return SSE发射器
     */
    SseEmitter createBuildStatusEmitter(Long appId);
    
    /**
     * 推送构建开始事件
     *
     * @param appId 应用ID
     */
    void pushBuildStarted(Long appId);
    
    /**
     * 推送构建成功事件
     *
     * @param appId 应用ID
     */
    void pushBuildSuccess(Long appId);
    
    /**
     * 推送构建失败事件
     *
     * @param appId 应用ID
     * @param error 错误信息
     */
    void pushBuildFailure(Long appId, String error);
}
