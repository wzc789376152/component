package com.github.wzc789376152.shiro.realm;

import com.github.wzc789376152.shiro.service.IShiroService;
import com.github.wzc789376152.vo.UserInfo;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class ShiroPasswordRealm extends AuthorizingRealm {

    private Logger logger = LoggerFactory.getLogger(ShiroPasswordRealm.class);

    @Autowired(required = false)
    private IShiroService shiroService;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof UsernamePasswordToken;
    }

    /**
     * 登录认证
     *
     * @return AuthenticationInfo
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken)
            throws AuthenticationException {
        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
        // 若存在，将此用户存放到登录认证info中，无需自己做密码对比，Shiro会为我们进行密码对比校验
        return new SimpleAuthenticationInfo(shiroService.findUserInfoByUsername(token.getUsername(),token.getHost()), shiroService.findPasswordByUsername(token.getUsername(),token.getHost()), ByteSource.Util.bytes(shiroService.findSaltByUsername(token.getUsername(),token.getHost())), "Password");
    }

    /**
     * 权限认证
     *
     * @return AuthorizationInfo
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        List<String> roles = shiroService.findRolesByObject((UserInfo) principalCollection.getPrimaryPrincipal());
        if (roles == null) {
            roles = new ArrayList<>();
        }
        info.addRoles(roles);
        List<String> permissions = shiroService.findPermissionsByObject((UserInfo) principalCollection.getPrimaryPrincipal());
        if (permissions == null) {
            permissions = new ArrayList<>();
        }
        info.addStringPermissions(permissions);
        return info;
    }

    public void clearAuthenticationInfo() {
        // 清除验证信息
        PrincipalCollection principals = SecurityUtils.getSubject().getPrincipals();
        super.clearCache(principals);
    }
}
