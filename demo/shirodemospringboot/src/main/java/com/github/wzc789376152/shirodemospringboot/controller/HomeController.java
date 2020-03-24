package com.github.wzc789376152.shirodemospringboot.controller;

import com.github.wzc789376152.shiro.properties.ShiroProperty;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
public class HomeController {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ShiroProperty shiroProperty;

    @RequestMapping("")
    public String index() {
        return "/index";
    }

    @GetMapping("login")
    public String login() {
        return "/login";
    }

    @PostMapping("login")
    public String login(String username, String password) {
        UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(
                username,
                password,
                true);
        SecurityUtils.getSubject().login(usernamePasswordToken);
        return "/index";
    }

    @RequestMapping("register")
    public String register(String username, String password) {
        String salt = UUID.randomUUID().toString();
        Md5Hash md5Hash = new Md5Hash(password, salt, shiroProperty.getHashIterations());
        password = md5Hash.toString();
        jdbcTemplate.update("insert into user(id,username,password,salt) values(?,?,?,?)", UUID.randomUUID().toString(), username, password, salt);
        return "/login";
    }
}
