package com.jamin.codecube.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
}