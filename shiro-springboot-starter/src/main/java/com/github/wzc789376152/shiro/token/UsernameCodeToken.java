package com.github.wzc789376152.shiro.token;

import org.apache.shiro.authc.UsernamePasswordToken;

public class UsernameCodeToken extends UsernamePasswordToken {
    public UsernameCodeToken() {
        super();
    }
    public UsernameCodeToken(String username, String code) {
        super(username, code);
    }

    public UsernameCodeToken(String username, String code, boolean rememberMe) {
        super(username, code, rememberMe);
    }
}
