package com.yupi.yuaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yupi.yuaicodemother.model.dto.user.UserQueryRequest;
import com.yupi.yuaicodemother.model.entity.User;
import com.yupi.yuaicodemother.model.vo.LoginUserVO;
import com.yupi.yuaicodemother.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/JaminLiu020">程序员小明</a>
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册。
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    Long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取加密后的密码。
     *
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录。
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户的脱敏信息。
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录用户。
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销。
     * @param request
     * @return
     */
    boolean logout(HttpServletRequest request);

    /**
     * 获取脱敏后的用户信息。
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息列表。
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件包装器。
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

}
