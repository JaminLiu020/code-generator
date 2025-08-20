package com.yupi.yuaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yupi.yuaicodemother.model.dto.app.AppQueryRequest;
import com.yupi.yuaicodemother.model.entity.App;
import com.yupi.yuaicodemother.model.entity.User;
import com.yupi.yuaicodemother.model.vo.AppVO;
import org.springframework.http.codec.ServerSentEvent;
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
    Flux<ServerSentEvent<String>> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 部署应用
     * @param appId
     * @param loginUser
     * @return
     */
    String deployApp(Long appId, User loginUser);

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
}
