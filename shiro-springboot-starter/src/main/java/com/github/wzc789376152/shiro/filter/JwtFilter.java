package com.github.wzc789376152.shiro.filter;

import com.github.wzc789376152.shiro.properties.ShiroJwtProperty;
import com.github.wzc789376152.shiro.token.JwtToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;


public class JwtFilter extends BasicHttpAuthenticationFilter {
    ShiroJwtProperty shiroJwtProperty;

    public JwtFilter(ShiroJwtProperty shiroJwtProperty) {
        this.shiroJwtProperty = shiroJwtProperty;
    }

    /**
     * 执行登录认证
     *
     * @param request     ServletRequest
     * @param response    ServletResponse
     * @param mappedValue mappedValue
     * @return 是否成功
     */
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        String token = ((HttpServletRequest) request).getHeader(shiroJwtProperty.getHeader());
        if (token != null) {
            return executeLogin(request, response);
        }
        // 如果请求头不存在 Token，则可能是执行登陆操作或者是游客状态访问，无需检查 token，直接返回 true
        return true;
    }

    /**
     * 执行登录
     */
    @Override
    protected boolean executeLogin(ServletRequest request, ServletResponse response) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String token = httpServletRequest.getHeader(shiroJwtProperty.getHeader());
        JwtToken jwtToken = new JwtToken(token);
        // 提交给realm进行登入，如果错误他会抛出异常并被捕获
        try {
            getSubject(request, response).login(jwtToken);
        } catch (IncorrectCredentialsException e) {
            try {
                response.getWriter().println("用户已过期！");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            return false;
        }
        // 如果没有抛出异常则代表登入成功，返回true
        return true;
    }

    /**
     * 对跨域提供支持
     */
    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
        return super.preHandle(request, response);
    }
}
