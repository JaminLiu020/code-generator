package com.jamin.codecube.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.jamin.codecube.annotation.AuthCheck;
import com.jamin.codecube.common.BaseResponse;
import com.jamin.codecube.common.ResultUtils;
import com.jamin.codecube.constant.UserConstant;
import com.jamin.codecube.model.dto.ChatHistoryQueryRequest;
import com.jamin.codecube.model.entity.User;
import com.jamin.codecube.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.jamin.codecube.model.entity.ChatHistory;
import com.jamin.codecube.service.ChatHistoryService;

import java.time.LocalDateTime;

/**
 *  控制层。
 *
 * @author <a href="https://github.com/JaminLiu020">程序员小明</a>
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;
    @Autowired
    private UserService userService;

    /**
     * 查询应用的聊天记录列表
     * @param appId
     * @param pageSize
     * @param lastCreateTime
     * @param request
     * @return
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> chatHistoryPage = chatHistoryService.listChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(chatHistoryPage);
    }

    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new IllegalArgumentException("查询请求不能为空");
        }
        int pageNum = queryRequest.getPageNum();
        int pageSize = queryRequest.getPageSize();
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(queryRequest);
        Page<ChatHistory> chatHistoryPage = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(chatHistoryPage);
    }
}
