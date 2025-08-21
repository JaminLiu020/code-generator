package com.jamin.codecube.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jamin.codecube.model.enums.ChatHistoryMessageTypeEnum;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.jamin.codecube.constant.UserConstant;
import com.jamin.codecube.exception.ErrorCode;
import com.jamin.codecube.exception.ThrowUtils;
import com.jamin.codecube.model.dto.ChatHistoryQueryRequest;
import com.jamin.codecube.model.entity.App;
import com.jamin.codecube.model.entity.ChatHistory;
import com.jamin.codecube.mapper.ChatHistoryMapper;
import com.jamin.codecube.model.entity.User;
import com.jamin.codecube.service.AppService;
import com.jamin.codecube.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/JaminLiu020">程序员小明</a>
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

    @Autowired
    @Lazy
    private AppService appService;
    /**
     * 添加聊天记录
     *
     * @param appId       应用ID
     * @param message     消息内容
     * @param messageType 消息类型
     * @param userId      用户ID
     * @return 是否添加成功
     */
    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        // 验证消息类型是否有效
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的消息类型: " + messageType);
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        return this.save(chatHistory);
    }

    /**
     * 根据应用ID删除聊天记录
     * @param appId
     * @return
     */
    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

    @Override
    public Page<ChatHistory> listChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize >50, ErrorCode.PARAMS_ERROR, "分页大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        // 验证权限，只有用户和管理员可以查看
        App app = appService.getById(appId);
        Boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = loginUser.getId().equals(app.getUserId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权限查看该应用的聊天记录");

        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    /**
     * 构造查询条件
     * @param queryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest queryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (queryRequest == null)
            return queryWrapper;
        Long id = queryRequest.getId();
        String message = queryRequest.getMessage();
        String messageType = queryRequest.getMessageType();
        Long appId = queryRequest.getAppId();
        Long userId = queryRequest.getUserId();
        LocalDateTime lastCreateTime = queryRequest.getLastCreateTime();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("id", id, id != null && id > 0)
                .like("message", message)
                .eq("messageType", messageType, messageType != null)
                .eq("appId", appId)
                .eq("userId", userId)
                .lt("createTime", lastCreateTime, lastCreateTime != null);
        // 设置排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equalsIgnoreCase(sortOrder));
        }
        else{
            queryWrapper.orderBy("createTime", false); // 默认按创建时间降序
        }
        return queryWrapper;
    }

    /**
     * 加载聊天记录到内存中
     * @param appId
     * @param chatMemory
     * @param maxCount
     * @return
     */
    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount){
        try {
            // 构造查询条件，起始点为1，而不是0，用于排除最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> chatHistoryList = this.list(queryWrapper);
            if(CollUtil.isEmpty(chatHistoryList)){
                return 0;
            }
            // 记录加载成功的条数
            int loadedCount = 0;
            // 清理历史缓存，避免重复加载
            chatMemory.clear();
            // 倒序插入到内存中，保证顺序正确
            for (ChatHistory chatHistory : chatHistoryList.reversed()) {
                if (chatHistory.getMessageType().equals(ChatHistoryMessageTypeEnum.USER.getValue())) {
                    chatMemory.add(UserMessage.from(chatHistory.getMessage()));
                } else if (chatHistory.getMessageType().equals(ChatHistoryMessageTypeEnum.AI.getValue())) {
                    chatMemory.add(AiMessage.from(chatHistory.getMessage()));
                }
                loadedCount++;
            }
            log.info("从数据库加载聊天记录，appId: {}, 加载条数: {}", appId, loadedCount);
            return loadedCount;
        }
        catch (Exception e){
            log.error("从数据库加载聊天记录异常，appId: {}", appId, e.getMessage());
            return 0;
        }
    }

}
