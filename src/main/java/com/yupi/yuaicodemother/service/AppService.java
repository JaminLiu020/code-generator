package com.yupi.yuaicodemother.service;

import com.mybatisflex.core.service.IService;
import com.yupi.yuaicodemother.model.entity.App;
import com.yupi.yuaicodemother.model.vo.AppVO;

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
}
