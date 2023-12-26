package com.github.wzc789376152.shiro.realm;

import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.wzc789376152.shiro.service.IJwtService;
import com.github.wzc789376152.shiro.service.IShiroService;
import com.github.wzc789376152.shiro.token.JwtToken;
import com.github.wzc789376152.vo.UserInfo;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;


public class ShiroJwtRealm extends AuthorizingRealm {
    @Autowired(required = false)
    private IJwtService jwtService;
    @Autowired(required = false)
    private IShiroService shiroService;


    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    /**
     * 只有当需要检测用户权限的时候才会调用此方法，例如checkRole,checkPermission之类的
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        List<String> roles = shiroService.findRolesByObject((UserInfo) principals.getPrimaryPrincipal());
        if (roles == null) {
            roles = new ArrayList<>();
        }
        authorizationInfo.addRoles(roles);
        List<String> permissions = shiroService.findPermissionsByObject((UserInfo) principals.getPrimaryPrincipal());
        if (permissions == null) {
            permissions = new ArrayList<>();
        }
        authorizationInfo.addStringPermissions(permissions);
        return authorizationInfo;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken auth) {
        String token = (String) auth.getCredentials();
        boolean verify = jwtService.verify(token);
        if (!verify) {
            throw new TokenExpiredException("token已失效");
        }
        return new SimpleAuthenticationInfo(jwtService.getUserInfo(token), token, "Jwt");
    }

    public void clearAuthenticationInfo() {
        // 清除验证信息
        PrincipalCollection principals = SecurityUtils.getSubject().getPrincipals();
        super.clearCache(principals);
    }
}
