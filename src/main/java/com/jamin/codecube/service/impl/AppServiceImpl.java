package com.jamin.codecube.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.jamin.codecube.ai.AiCodeGenTypeRoutingService;
import com.jamin.codecube.ai.AiCodeGenTypeRoutingServiceFactory;
import com.jamin.codecube.common.DeleteRequest;
import com.jamin.codecube.constant.AppConstant;
import com.jamin.codecube.constant.UserConstant;
import com.jamin.codecube.core.AiCodeGeneratorFacade;
import com.jamin.codecube.core.builder.VueProjectBuilder;
import com.jamin.codecube.core.handler.StreamHandlerExecutor;
import com.jamin.codecube.langgraph4j.service.CodeGenWorkflowService;
import com.jamin.codecube.mapper.AppMapper;
import com.jamin.codecube.model.dto.app.AppAddRequest;
import com.jamin.codecube.model.dto.app.AppQueryRequest;
import com.jamin.codecube.model.entity.App;
import com.jamin.codecube.model.entity.User;
import com.jamin.codecube.model.enums.ChatHistoryMessageTypeEnum;
import com.jamin.codecube.model.enums.CodeGenTypeEnum;
import com.jamin.codecube.model.vo.AppVO;
import com.jamin.codecube.model.vo.UserVO;
import com.jamin.codecube.monitor.MonitorContext;
import com.jamin.codecube.monitor.MonitorContextHolder;
import com.jamin.codecube.service.*;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.jamin.codecube.exception.BusinessException;
import com.jamin.codecube.exception.ErrorCode;
import com.jamin.codecube.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/JaminLiu020">程序员小明</a>
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService {

    @Autowired
    private UserService userService;
    @Autowired
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Autowired
    private ChatHistoryService chatHistoryService;
    @Autowired
    private ChatHistoryOriginalService chatHistoryOriginalService;
    @Autowired
    private StreamHandlerExecutor streamHandlerExecutor;
    @Autowired
    private VueProjectBuilder vueProjectBuilder;
    @Autowired
    private ScreenshotService screenshotService;
    @Autowired
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;
    @Autowired
    private CodeGenWorkflowService codeGenWorkflowService;
    @Value("${code.deploy-host:http://localhost}")
    private String deployHost;


    /**
     * 获取应用的视图对象。
     * @param app
     * @return
     */
    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    /**
     * 构建查询条件
     * @param appQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 获取应用视图对象列表
     * @param appList
     * @return
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    /**
     * 通过对话生成应用代码
     *
     * @param appId
     * @param message
     * @param loginUser
     * @param agent
     * @return
     */
    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser, boolean agent) {
        // 1.参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息不能为空");
        // 2.获取应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3.校验用户权限
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()),
                ErrorCode.FORBIDDEN_ERROR, "没有权限操作该应用");
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的代码生成类型");
        // 4.在调用AI前，将用户消息存进对话记录表
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        chatHistoryOriginalService.addOriginalChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 5.设置监控上下文
        MonitorContextHolder.setContext(
                MonitorContext.builder()
                        .appId(String.valueOf(appId))
                        .userId(String.valueOf(loginUser.getId()))
                .build()
        );
        // 6. 根据 agent 参数选择生成方式
        Flux<String> codeStream;
        if (agent) {
            // Agent 模式：使用工作流生成代码
            codeStream = codeGenWorkflowService.executeWorkflowWithFlux(message, appId);
        } else {
            // 传统模式：调用 AI 生成代码（流式）
            codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        }
        // 7.收集AI响应的内容，并且在完成后保存记录到对话历史
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, chatHistoryOriginalService, appId, loginUser, codeGenTypeEnum)
                .doFinally(signalType -> {
                    // 流结束时清理监控上下文，无论成功还是失败
                    MonitorContextHolder.clearContext();
                });
    }

    /**
     * 部署应用
     * @param appId
     * @param loginUser
     * @return
     */
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 校验应用是否该用户所有
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "没有权限操作该应用");
        // 4. 检查该应用是否已有deployKey
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(app.getDeployKey())) {
//            deployKey = RandomUtil.randomString(6);
            deployKey = getDeployKey();
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "源代码目录不存在");
        }
        // 7. Vue 项目特殊处理，先构建再部署
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // 执行构建
            boolean builtSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!builtSuccess, ErrorCode.SYSTEM_ERROR, "Vue项目构建失败");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath + File.separator + "dist");
            ThrowUtils.throwIf(!distDir.exists() || !distDir.isDirectory(),
                    ErrorCode.NOT_FOUND_ERROR, "Vue 项目构建完成，但未生成 dist 目录");
            // dist 目录作为部署源
            sourceDir = distDir;
            log.info("Vue 项目构建成功，将部署 dist 目录：{}", distDir.getAbsolutePath());
        }
        // 8. 复制源代码到部署目录
        try{
            FileUtil.copyContent(sourceDir,
                    new File(AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey),
                    true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败，无法更新应用信息");
        }
        // 9. 更新deployKey和deployTime
        App updateApp = App.builder()
                            .id(appId)
                            .deployKey(deployKey)
                            .deployedTime(LocalDateTime.now())
                            .build();
        boolean updateResult = updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.SYSTEM_ERROR, "部署失败，无法更新应用信息");
        // 10. 构建应用访问 URL
        String appDeployUrl = String.format("%s/%s/", deployHost, deployKey);
        // 11. 异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    /**
     * 异步生成应用截图并更新应用封面
     * @param appId
     * @param appDeployUrl
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appDeployUrl) {
        Thread.startVirtualThread(() -> {
            try {
                // 生成应用截图
                String screenshotUrl = screenshotService.generateAndUploadScreenshot(appDeployUrl);
                if (StrUtil.isNotBlank(screenshotUrl)) {
                    // 更新应用封面
                    App updateApp = App.builder()
                            .id(appId)
                            .cover(screenshotUrl)
                            .build();
                    boolean updateResult = updateById(updateApp);
                    if (!updateResult) {
                        log.error("更新应用封面失败，应用ID: {}", appId);
                    } else {
                        log.info("应用封面更新成功: {}", screenshotUrl);
                    }
                } else {
                    log.error("生成应用截图失败，应用ID: {}", appId);
                }
            } catch (Exception e) {
                log.error("异步生成应用截图时发生异常: {}", e.getMessage());
            }
        });
    }

    /**
     * 生成deployKey
     * @return
     */
    @Override
    public String getDeployKey() {
        String deployKey = RandomUtil.randomString(6);
        if (this.getByDeployKey(deployKey).size()>0){
            deployKey = getDeployKey();
        }

        return deployKey;
    }

    /**
     * 根据deployKey获取应用
     * @param deployKey
     * @return
     */
    @Override
    public List<App> getByDeployKey(String deployKey) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("deployKey", deployKey);
        return this.list(queryWrapper);
    }

    /**
     * 删除应用时关联删除对话历史
     *
     * @param id 应用ID
     * @return 是否成功
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
            chatHistoryOriginalService.deleteByAppId(appId);
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }

    /**
     * 创建 App
     * @param appAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser){
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");

        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 使用 AI 智能选择代码生成类型（多例模式）
        AiCodeGenTypeRoutingService routingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum codeGenTypeEnum = routingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(codeGenTypeEnum.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID：{}，生成类型：{}", app.getId(), codeGenTypeEnum.getValue());
        return app.getId();
    }

    /**
     * 删除 App，并删除项目文件、部署文件和封面文件
     * @param deleteRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean deleteApp(DeleteRequest deleteRequest, User loginUser) {

        long id = deleteRequest.getId();
        // 判断 App 是否存在
        App oldApp = this.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldApp.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 将对应的App条目的is_deleted置为1
        App newApp = App.builder()
                .id(oldApp.getId())
                .isDelete(AppConstant.IS_DELETED)
                .build();
        boolean result = this.updateById(newApp, true);

        return result;
    }
}
