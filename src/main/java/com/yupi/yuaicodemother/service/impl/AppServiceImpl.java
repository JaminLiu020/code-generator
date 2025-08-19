package com.yupi.yuaicodemother.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.yuaicodemother.model.entity.App;
import com.yupi.yuaicodemother.mapper.AppMapper;
import com.yupi.yuaicodemother.service.AppService;
import org.springframework.stereotype.Service;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/JaminLiu020">程序员小明</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

}
