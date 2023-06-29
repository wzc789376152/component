package com.github.wzc789376152.vo;


import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserInfo {
    /**
     * 用户id
     */
    private Serializable id;
    /**
     * 用户创建时间
     */
    private Date createTime;
    /**
     * 用户名称
     */
    private String userName;
    /**
     * 来源
     */
    private String host;
    /**
     * token
     */
    private String token;
}

