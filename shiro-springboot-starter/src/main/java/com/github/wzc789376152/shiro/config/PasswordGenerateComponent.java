package com.github.wzc789376152.shiro.config;

import com.github.wzc789376152.shiro.properties.ShiroProperty;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PasswordGenerateComponent {
    @Autowired
    private ShiroProperty shiroProperty;

    /**
     * 加密密码
     *
     * @param password 密码明文
     * @param salt     盐
     * @return String
     */
    public String generatePassword(String password, String salt) {
        Md5Hash md5Hash = new Md5Hash(password, salt, shiroProperty.getHashIterations());
        return md5Hash.toString();
    }
}
