package com.github.wzc789376152.shiro.realm;

import java.io.Serializable;

public class UserInfo implements Serializable {
    private Serializable id;
    private String username;

    public Serializable getId() {
        return id;
    }

    public void setId(Serializable id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
