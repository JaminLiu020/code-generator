package com.jamin.codecube.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jamin.codecube.mapper.UserMapper;
import com.jamin.codecube.model.enums.UserRoleEnum;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.jamin.codecube.constant.UserConstant;
import com.jamin.codecube.exception.BusinessException;
import com.jamin.codecube.exception.ErrorCode;
import com.jamin.codecube.exception.ThrowUtils;
import com.jamin.codecube.model.dto.user.UserQueryRequest;
import com.jamin.codecube.model.entity.User;
import com.jamin.codecube.model.vo.LoginUserVO;
import com.jamin.codecube.model.vo.UserVO;
import com.jamin.codecube.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/JaminLiu020">程序员小明</a>
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService {

    /**
     * 用户注册。
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    @Override
    public Long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 参数校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度不能小于4");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }

        // 2. 账号不能重复
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        // 3. 密码加密
        String encryptPassword = getEncryptPassword(userPassword);

        // 4. 创建用户
        User user = User.builder()
                .userAccount(userAccount)
                .userPassword(encryptPassword)
                .userName("用户" + userAccount) // 默认用户名
                .userRole(UserRoleEnum.USER.getValue()) // 默认普通用户
                .shareCode(userAccount)
                .build();

        boolean saveResult = save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户注册失败，数据库错误");
        }
        return user.getId();
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        final String SALT = "jamin";

        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 用户登陆。
     * @param userAccount
     * @param userPassword
     * @return
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度不能小于4");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8");
        }

        // 2. 查询用户
        String encryptPassword = getEncryptPassword(userPassword);
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount)
                .eq("userPassword", encryptPassword);
        User user = this.getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不存在或密码错误");
        }

        // 3. 记录登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);

        // 4. 返回脱敏后的用户信息
        return this.getLoginUserVO(user);
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取当前登录用户。
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断用户是否登录
        User currentUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (currentUser == null || currentUser.getId() == null) {
            // 如果用户未登录，抛出异常
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }

        // 从数据库查询当前用户信息
        currentUser = this.getById(currentUser.getId());
        if (currentUser == null) {
            // 如果用户信息不存在，抛出异常
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户信息不存在");
        }
        return currentUser;
    }

    /**
     * 用户注销。
     * @param request
     * @return
     */
    @Override
    public boolean logout(HttpServletRequest request) {
        // 1. 判断是否登录
        User currentUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (currentUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }

        // 2. 注销
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);

        return true;
    }

    /**
     * 获取脱敏后的用户信息。
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取脱敏后的用户信息列表。
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        ThrowUtils.throwIf(CollUtil.isEmpty(userList), ErrorCode.PARAMS_ERROR, "用户列表不能为空");
        return userList.stream()
                .map(this::getUserVO)
                .toList();
    }

    /**
     * 获取查询条件包装器。
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        QueryWrapper queryWrapper = new QueryWrapper();
        return QueryWrapper.create()
                .eq("id", userQueryRequest.getId())
                .eq("userRole", userQueryRequest.getUserRole())
                .like("userAccount", userQueryRequest.getUserAccount())
                .like("userName", userQueryRequest.getUserName())
                .like("userProfile", userQueryRequest.getUserProfile())
                .orderBy(userQueryRequest.getSortField(), "ascend".equals(userQueryRequest.getSortOrder()));
    }
}
