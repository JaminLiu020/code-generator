package com.jamin.codecube.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.jamin.codecube.model.dto.ChatHistoryQueryRequest;
import com.jamin.codecube.model.entity.ChatHistory;
import com.jamin.codecube.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/JaminLiu020">程序员小明</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加聊天记录
     * @param appId
     * @param message
     * @param messageType
     * @param userId
     * @return
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 根据应用ID删除聊天记录
     * @param appId
     * @return
     */
    boolean deleteByAppId(Long appId);

    /**
     * 查询应用的聊天记录列表
     * @param appId
     * @param pageSize
     * @param lastCreateTime
     * @param loginUser
     * @return
     */
    Page<ChatHistory> listChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser);

    /**
     * 获取查询包装类
     * @param queryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest queryRequest);

    /**
     * 加载聊天记录到内存
     * @param appId
     * @param chatMemory
     * @param maxCount
     * @return
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
