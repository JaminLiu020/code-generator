package com.yupi.yuaicodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.yupi.yuaicodemother.annotation.AuthCheck;
import com.yupi.yuaicodemother.common.BaseResponse;
import com.yupi.yuaicodemother.common.DeleteRequest;
import com.yupi.yuaicodemother.common.ResultUtils;
import com.yupi.yuaicodemother.constant.UserConstant;
import com.yupi.yuaicodemother.exception.BusinessException;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.exception.ThrowUtils;
import com.yupi.yuaicodemother.model.dto.user.UserAddRequest;
import com.yupi.yuaicodemother.model.dto.user.UserQueryRequest;
import com.yupi.yuaicodemother.model.dto.user.UserRegisterRequest;
import com.yupi.yuaicodemother.model.dto.user.UserUpdateRequest;
import com.yupi.yuaicodemother.model.vo.LoginUserVO;
import com.yupi.yuaicodemother.model.vo.UserVO;
import com.yupi.yuaicodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import com.yupi.yuaicodemother.model.entity.User;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 *  控制层。
 *
 * @author <a href="https://github.com/JaminLiu020">程序员小明</a>
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 添加用户。
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        //1. 参数校验
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2.拷贝到实体类
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        //3.设置默认密码
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);

        //4.保存用户
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "用户添加失败");

        return ResultUtils.success(user.getId());
    }

    /**
     * 删除用户。
     *
     * @param deleteRequest 删除请求
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        // 参数校验
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        // 删除用户
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户信息。
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        // 参数校验
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR, "请求体不能为空");
        ThrowUtils.throwIf(userUpdateRequest.getId() == null || userUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空或小于等于0");
        // 拷贝到实体类
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        // 更新用户

//        String userName = user.getUserName();
//        String userAvatar = user.getUserAvatar();
//        String userProfile = user.getUserProfile();
//        String userRole = user.getUserRole();
//        boolean b = UpdateChain.of(User.class)
//                .set("userName", userName, StrUtil.isNotBlank(userName))
//                .set("userAvatar", userAvatar, StrUtil.isNotBlank(userAvatar))
//                .set("userProfile", userProfile, StrUtil.isNotBlank(userProfile))
//                .set("userRole", userRole, StrUtil.isNotBlank(userRole))
//                .where("id", user.getId())
//                .update();
        boolean b = userService.updateById(user);
        // 更新失败
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR, "用户更新失败");
        return ResultUtils.success(true);
    }


    /**
     * 根据 ID 获取用户信息（仅管理员可用）。
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long id) {
        // 参数校验
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空或小于等于0");
        // 获取用户
        User user = userService.getById(id);
        // 用户不存在
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        return ResultUtils.success(user);
    }

    /**
     * 根据 ID 获取用户视图对象。
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(Long id) {
        // 获取用户
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        // 返回用户视图对象
        return ResultUtils.success(userService.getUserVO(user));
    }


    /**
     * 分页获取用户列表（仅管理员可用）。
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        // 分页查询
        int pageNum = userQueryRequest.getPageNum();
        int pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize), userService.getQueryWrapper(userQueryRequest));
        // 转换为 UserVO
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        // 返回结果
        return ResultUtils.success(userVOPage);
    }

    /**
     * 用户注册接口
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        Long userId = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(userId);
    }

    /**
     * 用户登录接口
     * @param userRegisterRequest
     * @param request
     * @return
     */
    @PostMapping("login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserRegisterRequest userRegisterRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户信息
     * @param request
     * @return
     */
    @GetMapping("get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        LoginUserVO loginUserVO = userService.getLoginUserVO(userService.getLoginUser(request));
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户注销接口
     * @param request
     * @return
     */
    @PostMapping("logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if(request==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.logout(request);
        return ResultUtils.success(result);
    }

}
