package com.github.wzc789376152.shiro.realm;

import com.github.wzc789376152.shiro.service.IShiroCodeService;
import com.github.wzc789376152.shiro.service.IShiroService;
import com.github.wzc789376152.shiro.token.UsernameCodeToken;
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

public class ShiroCodeRealm extends AuthorizingRealm {
    private Logger logger = LoggerFactory.getLogger(ShiroPasswordRealm.class);
    @Autowired
    private IShiroCodeService shiroCodeService;
    @Autowired
    private IShiroService shiroService;
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof UsernameCodeToken;
    }
    /**
     * 登录认证
     *
     * @param authenticationToken
     * @return
     * @throws AuthenticationException
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken)
            throws AuthenticationException {
        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
        // 若存在，将此用户存放到登录认证info中，无需自己做密码对比，Shiro会为我们进行密码对比校验
        return new SimpleAuthenticationInfo(shiroService.findUserInfoByUsername(token.getUsername()), shiroCodeService.get(token.getUsername()), ByteSource.Util.bytes(shiroService.findSaltByUsername(token.getUsername())), "Code");
    }

    /**
     * 权限认证
     *
     * @param principalCollection
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.addRoles(shiroService.findRolesByObject((UserInfo)principalCollection.getPrimaryPrincipal()));
        info.addStringPermissions(shiroService.findPermissionsByObject((UserInfo)principalCollection.getPrimaryPrincipal()));
        return info;
    }

    public void clearAuthenticationInfo() {
        // 清除验证信息
        PrincipalCollection principals = SecurityUtils.getSubject().getPrincipals();
        super.clearCache(principals);
    }
}
