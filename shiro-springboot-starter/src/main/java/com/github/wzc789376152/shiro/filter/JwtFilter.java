package com.github.wzc789376152.shiro.filter;

import com.github.wzc789376152.shiro.properties.ShiroJwtProperty;
import com.github.wzc789376152.shiro.properties.ShiroProperty;
import com.github.wzc789376152.shiro.token.JwtToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;


public class JwtFilter extends BasicHttpAuthenticationFilter {
    ShiroJwtProperty shiroJwtProperty;
    ShiroProperty shiroProperty;

    public JwtFilter(ShiroJwtProperty shiroJwtProperty, ShiroProperty shiroProperty) {
        this.shiroJwtProperty = shiroJwtProperty;
        this.shiroProperty = shiroProperty;
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
        if (token == null) {
            Cookie[] cookies = ((HttpServletRequest) request).getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(shiroJwtProperty.getHeader())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }
        if (token != null) {
            return executeLogin(request, response);
        }
        responseError(response, "用户未登录");
        return false;
    }

    /**
     * 执行登录
     */
    @Override
    protected boolean executeLogin(ServletRequest request, ServletResponse response) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String token = httpServletRequest.getHeader(shiroJwtProperty.getHeader());
        if (token == null) {
            Cookie[] cookies = ((HttpServletRequest) request).getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(shiroJwtProperty.getHeader())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }
        JwtToken jwtToken = new JwtToken(token);
        // 提交给realm进行登入，如果错误他会抛出异常并被捕获
        try {
            getSubject(request, response).login(jwtToken);
        } catch (IncorrectCredentialsException e) {
            responseError(response, "用户未登录");
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

    /**
     * 请求异常则需要重新登录
     */
    private void responseError(ServletResponse response, String message) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        ((HttpServletResponse) response).setStatus(401);
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            out.write(message.getBytes());
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
