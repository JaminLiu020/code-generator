package com.jamin.codecube.service;

import com.jamin.codecube.common.BaseResponse;
import com.jamin.codecube.common.DeleteRequest;
import com.jamin.codecube.model.dto.app.AppAddRequest;
import com.jamin.codecube.model.dto.app.AppQueryRequest;
import com.jamin.codecube.model.entity.App;
import com.jamin.codecube.model.entity.User;
import com.jamin.codecube.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/JaminLiu020">程序员小明</a>
 */
public interface AppService extends IService<App> {

    /**
     * 获取应用的视图对象。
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 构建查询条件
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取应用视图对象列表
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 通过对话生成应用代码
     *
     * @param appId
     * @param message
     * @param loginUser
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 部署应用
     * @param appId
     * @param loginUser
     * @return
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 异步生成应用截图并更新应用封面
     * @param appId
     * @param appDeployUrl
     */
    void generateAppScreenshotAsync(Long appId, String appDeployUrl);

    /**
     * 生成deployKey
     * @return
     */
    String getDeployKey();

    /**
     * 根据deployKey获取应用
     * @param deployKey
     * @return
     */
    List<App> getByDeployKey(String deployKey);


    /**
     * 创建 App
     * @param appAddRequest
     * @param user
     * @return
     */
    Long createApp(AppAddRequest appAddRequest, User user);

    /**
     * 删除 App 并清理项目文件、部署文件和封面文件
     * @param deleteRequest
     * @param loginUser
     * @return
     */
    boolean deleteApp(DeleteRequest deleteRequest, User loginUser);
}
