package com.github.wzc789376152.shiro.filter;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.github.wzc789376152.service.IResponseService;
import com.github.wzc789376152.shiro.properties.ShiroJwtProperty;
import com.github.wzc789376152.shiro.properties.ShiroProperty;
import com.github.wzc789376152.shiro.token.JwtToken;
import com.github.wzc789376152.utils.IpUtil;
import com.github.wzc789376152.utils.JSONUtils;
import com.github.wzc789376152.utils.TokenUtils;
import com.github.wzc789376152.vo.UserInfo;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


public class JwtFilter extends BasicHttpAuthenticationFilter {
    private Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    ShiroJwtProperty shiroJwtProperty;
    ShiroProperty shiroProperty;
    IResponseService responseService;

    public JwtFilter(ShiroJwtProperty shiroJwtProperty, ShiroProperty shiroProperty, IResponseService responseService) {
        this.shiroJwtProperty = shiroJwtProperty;
        this.shiroProperty = shiroProperty;
        this.responseService = responseService;
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
        return executeLogin(request, response);
    }

    /**
     * 执行登录
     */
    @Override
    protected boolean executeLogin(ServletRequest request, ServletResponse response) {
        List<String> keyArray = shiroJwtProperty.getHeaders();
        boolean isLogin = false;
        boolean isTimeout = false;
        String token = null;
        for (String key : keyArray) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            token = httpServletRequest.getHeader(key);
            if (token == null) {
                Cookie[] cookies = ((HttpServletRequest) request).getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if (cookie.getName().equals(key)) {
                            token = cookie.getValue();
                            break;
                        }
                    }
                }
            }
            if (token != null) {
                JwtToken jwtToken = new JwtToken(token);
                // 提交给realm进行登入，如果错误他会抛出异常并被捕获
                try {
                    getSubject(request, response).login(jwtToken);
                    isLogin = true;
                } catch (AuthenticationException e) {
                    if (e.getCause() instanceof TokenExpiredException) {
                        isTimeout = true;
                    }
                }
            }
            if (isLogin) {
                break;
            }
        }
        if (!isLogin && isTimeout) {
            responseError(response, 402, "token已过期");
            return false;
        }
        if (isLogin) {
            UserInfo userInfo = (UserInfo) getSubject(request, response);
            userInfo.setToken(token);
            TokenUtils.setUserInfo(userInfo);
            return true;
        }
        if (IpUtil.checkIp(shiroJwtProperty.getIpWhileList())) {
            return true;
        }
        responseError(response, 401, "用户未登录");
        return false;
    }

    /**
     * 对跨域提供支持
     */
    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
        return super.preHandle(request, response);
    }

    @Override
    public void afterCompletion(ServletRequest request, ServletResponse response, Exception exception) throws Exception {
        TokenUtils.remove();
    }

    /**
     * 请求异常则需要重新登录
     */
    private void responseError(ServletResponse response, int status, String message) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        ((HttpServletResponse) response).setStatus(status);
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            out.write(JSONUtils.toJSONString(responseService.error(status, message)).getBytes(StandardCharsets.UTF_8));
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
