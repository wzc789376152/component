package com.github.wzc789376152.shiro.service.impl;

import com.github.wzc789376152.shiro.config.PasswordGenerateComponent;
import com.github.wzc789376152.shiro.properties.ShiroCodeProperty;
import com.github.wzc789376152.shiro.service.IShiroCodeService;
import org.crazycake.shiro.RedisManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

public class CodeServiceImpl implements IShiroCodeService {
    private String prefix = "shiro:code:";
    @Autowired(required = false)
    private RedisManager redisManager;
    @Autowired
    private PasswordGenerateComponent passwordGenerateComponent;
    @Autowired(required = false)
    private ShiroCodeProperty shiroCodeProperty;
    private Map<String, String> codeMap = new HashMap<>();

    @Override
    public String get(String username) {
        if (redisManager != null) {
            byte[] bytes = redisManager.get((prefix + username).getBytes());
            if (bytes == null) {
                return null;
            }
            return new String(bytes);
        }
        return codeMap.get(prefix + username);
    }

    @Override
    public Boolean save(String username, String salt, String code) {
        String generateCode = passwordGenerateComponent.generatePassword(code, salt);
        if (redisManager != null) {
            Integer timeout = shiroCodeProperty.getTimeout();
            if (timeout == null) {
                timeout = redisManager.getTimeout();
            }
            redisManager.set((prefix + username).getBytes(), generateCode.getBytes(), timeout);
        }
        codeMap.put(prefix + username, generateCode);
        return true;
    }
}
