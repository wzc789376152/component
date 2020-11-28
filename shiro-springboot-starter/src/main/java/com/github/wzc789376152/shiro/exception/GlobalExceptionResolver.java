package com.github.wzc789376152.shiro.exception;

import com.github.wzc789376152.shiro.properties.ShiroProperty;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GlobalExceptionResolver implements HandlerExceptionResolver {
    @Autowired
    private ShiroProperty shiroProperty;

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ModelAndView mv;
        //进行异常判断。如果捕获异常请求跳转。
        if (ex instanceof UnauthorizedException) {
            mv = new ModelAndView("redirect:"+shiroProperty.getUnauthorizedUrl());
            mv.addObject("msg", "没有此角色权限！");
            return mv;
        } else if (ex instanceof UnauthenticatedException) {
            mv = new ModelAndView("redirect:"+shiroProperty.getUnauthorizedUrl());
            mv.addObject("msg", "没有此权限！");
            return mv;
        } else {
            mv = new ModelAndView("redirect:"+shiroProperty.getLoginUrl());
            ex.printStackTrace();
            mv.addObject("msg", "验证失败!");
            return mv;
        }
    }
}