package com.yupi.yuaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.yuaicodemother.constant.AppConstant;
import com.yupi.yuaicodemother.core.AiCodeGeneratorFacade;
import com.yupi.yuaicodemother.exception.BusinessException;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.exception.ThrowUtils;
import com.yupi.yuaicodemother.model.dto.app.AppQueryRequest;
import com.yupi.yuaicodemother.model.entity.App;
import com.yupi.yuaicodemother.mapper.AppMapper;
import com.yupi.yuaicodemother.model.entity.User;
import com.yupi.yuaicodemother.model.enums.CodeGenTypeEnum;
import com.yupi.yuaicodemother.model.vo.AppVO;
import com.yupi.yuaicodemother.model.vo.UserVO;
import com.yupi.yuaicodemother.service.AppService;
import com.yupi.yuaicodemother.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/JaminLiu020">程序员小明</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

    @Autowired
    private UserService userService;
    @Autowired
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

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
     * @return
     */
    @Override
    public Flux<ServerSentEvent<String>> chatToGenCode(Long appId, String message, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息不能为空");
        // 获取应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 校验用户权限
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()),
                ErrorCode.FORBIDDEN_ERROR, "没有权限操作该应用");
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的代码生成类型");
        // 生成代码流
        Flux<String> contentFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 返回生成的代码流
        ThrowUtils.throwIf(contentFlux == null, ErrorCode.SYSTEM_ERROR, "代码生成失败");
        return contentFlux
                .map(chunk -> {
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonStr = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonStr)
                            .build();
                })
                .concatWith(Mono.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }

    /**
     * 部署应用
     * @param appId
     * @param loginUser
     * @return
     */
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 查询应用信息
        App app = getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 校验应用是否该用户所有
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "没有权限操作该应用");
        // 检查该应用是否已有deployKey
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(app.getDeployKey())) {
//            deployKey = RandomUtil.randomString(6);
            deployKey = getDeployKey();
        }
        // 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "源代码目录不存在");
        }
        // 复制源代码到部署目录
        try{
            FileUtil.copyContent(sourceDir,
                    new File(AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey),
                    true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败，无法更新应用信息");
        }
        //更新deployKey和deployTime
        App updateApp = App.builder()
                            .id(appId)
                            .deployKey(deployKey)
                            .deployedTime(LocalDateTime.now())
                            .build();
        boolean updateResult = updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.SYSTEM_ERROR, "部署失败，无法更新应用信息");
        // 生成部署URL并返回
        return String.format("%s/%s", AppConstant.CODE_DEPLOY_ROOT_DIR, deployKey);

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


}
