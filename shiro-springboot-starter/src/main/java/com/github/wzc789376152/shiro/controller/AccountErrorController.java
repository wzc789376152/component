package com.github.wzc789376152.shiro.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

@RestController
@RequestMapping("/accountError")
public class AccountErrorController {
    @GetMapping("/unlogin")
    public String unlogin(@RequestParam(required = false, value = "msg") String msg, HttpServletResponse response) {
        response.setStatus(401);
        return msg == null ? "用户未登录" : msg;
    }

    @GetMapping("/timeout")
    public String timeout(@RequestParam(required = false, value = "msg") String msg, HttpServletResponse response) {
        response.setStatus(402);
        return msg == null ? "Token失效" : msg;
    }

    @GetMapping("/unauthorized")
    public String unauthorized(@RequestParam(required = false, value = "msg") String msg, HttpServletResponse response) {
        response.setStatus(403);
        return msg == null ? "无权限访问" : msg;
    }
}
