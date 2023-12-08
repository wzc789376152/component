package com.github.wzc789376152.shiro.controller;

import com.github.wzc789376152.service.IResponseService;
import com.github.wzc789376152.vo.RetResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/accountError")
public class AccountErrorController {
    @Autowired
    IResponseService responseService;
    @GetMapping("/unlogin")
    public Object unlogin(@RequestParam(required = false, value = "msg") String msg, HttpServletResponse response) {
        response.setStatus(401);
        return responseService.error(401, msg == null ? "用户未登录" : msg);
    }

    @GetMapping("/timeout")
    public Object timeout(@RequestParam(required = false, value = "msg") String msg, HttpServletResponse response) {
        response.setStatus(402);
        return responseService.error(402, msg == null ? "Token失效" : msg);
    }

    @GetMapping("/unauthorized")
    public Object unauthorized(@RequestParam(required = false, value = "msg") String msg, HttpServletResponse response) {
        response.setStatus(403);
        return responseService.error(403, msg == null ? "无权限访问" : msg);
    }
}
