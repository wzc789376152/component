package com.github.wzc789376152.shiro.realm;

import com.github.wzc789376152.shiro.service.IJwtService;
import com.github.wzc789376152.shiro.service.IShiroService;
import com.github.wzc789376152.shiro.token.JwtToken;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;


public class ShiroJwtRealm extends AuthorizingRealm {
    @Resource
    private IJwtService jwtService;
    @Autowired
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
        // 添加角色
        authorizationInfo.addRoles(shiroService.findRolesByObject((UserInfo) principals.getPrimaryPrincipal()));
        // 添加权限
        authorizationInfo.addStringPermissions(shiroService.findPermissionsByObject((UserInfo) principals.getPrimaryPrincipal()));
        return authorizationInfo;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken auth) throws AuthenticationException {
        String token = (String) auth.getCredentials();
        String userName = jwtService.getUserName(token);
        boolean verify = false;
        try {
            if (userName != null) {
                verify = jwtService.verify(token, userName, shiroService.findPasswordByUsername(userName));
            }
        } catch (Exception e) {
        }
        if (!verify) {
            throw new IncorrectCredentialsException();
        }
        return new SimpleAuthenticationInfo(shiroService.findUserInfoByUsername(userName), token, "Jwt");
    }

    public void clearAuthenticationInfo() {
        // 清除验证信息
        PrincipalCollection principals = SecurityUtils.getSubject().getPrincipals();
        super.clearCache(principals);
    }
}
