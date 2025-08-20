package com.yupi.yuaicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.yuaicodemother.constant.UserConstant;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.exception.ThrowUtils;
import com.yupi.yuaicodemother.model.dto.ChatHistoryQueryRequest;
import com.yupi.yuaicodemother.model.entity.App;
import com.yupi.yuaicodemother.model.entity.ChatHistory;
import com.yupi.yuaicodemother.mapper.ChatHistoryMapper;
import com.yupi.yuaicodemother.model.entity.User;
import com.yupi.yuaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.yupi.yuaicodemother.service.AppService;
import com.yupi.yuaicodemother.service.ChatHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/JaminLiu020">程序员小明</a>
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

    @Autowired
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

}
