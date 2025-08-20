package com.yupi.yuaicodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.yupi.yuaicodemother.common.BaseResponse;
import com.yupi.yuaicodemother.common.ResultUtils;
import com.yupi.yuaicodemother.model.entity.User;
import com.yupi.yuaicodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.yupi.yuaicodemother.model.entity.ChatHistory;
import com.yupi.yuaicodemother.service.ChatHistoryService;

import java.time.LocalDateTime;
import java.util.List;

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

}
