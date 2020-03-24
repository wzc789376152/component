package com.github.wzc789376152.shiro.config;

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
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShiroRealm extends AuthorizingRealm {

    private Logger logger = LoggerFactory.getLogger(ShiroRealm.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        logger.info("验证当前Subject时获取到token为：" + token.toString());
        // 查出是否有此用户
        Map<String, Object> hasUser = null;
        try {
            String driverType = jdbcTemplate.getDataSource().getConnection().getMetaData().getDatabaseProductName();
            if (driverType.equals("MySQL")) {
                hasUser = jdbcTemplate.queryForMap("select * from user where username = ?", token.getUsername());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (hasUser != null) {
            // 若存在，将此用户存放到登录认证info中，无需自己做密码对比，Shiro会为我们进行密码对比校验
            return new SimpleAuthenticationInfo(token.getUsername(), hasUser.get("password"), ByteSource.Util.bytes(hasUser.get("salt").toString()), getName());
        }
        return null;
    }

    /**
     * 权限认证
     *
     * @param principalCollection
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        logger.info("##################执行Shiro权限认证##################");
        // 获取当前登录输入的用户名，等价于(String)
        // principalCollection.fromRealm(getName()).iterator().next();
        // String loginName = (String)
        // super.getAvailablePrincipal(principalCollection);
        // 2分钟执行一遍
        Map<String, Object> user = null;
        try {
            String driverType = jdbcTemplate.getDataSource().getConnection().getMetaData().getDatabaseProductName();
            if (driverType.equals("MySQL")) {
                user = jdbcTemplate.queryForMap("select * from user where username = ?", principalCollection.getPrimaryPrincipal().toString());
            }
            if (user != null) {
                // 权限信息对象info,用来存放查出的用户的所有的角色（role）及权限（permission）
                SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
                // 用户的角色集合
                List<Map<String, Object>> roleList = null;
                if (driverType.equals("MySQL")) {
                    roleList = jdbcTemplate.queryForList("select * from role r,user_role ur where r.id = ur.role_id and ur.user_id=?", user.get("id"));
                }
                for (Map<String, Object> role : roleList) {
                    info.addRole(role.get("identifier").toString());
                    List<Map<String, Object>> permissionList = null;
                    if (driverType.equals("MySQL")) {
                        permissionList = jdbcTemplate.queryForList("select p.identifier from permission p,role_permission rp where p.id = rp.permission_id and rp.role_id = ?", role.get("id"));
                    }
                    for (Map<String, Object> permission : permissionList) {
                        info.addStringPermission(permission.get("identifier").toString());
                    }
                }
                return info;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 返回null的话，就会导致任何用户访问被拦截的请求时，都会自动跳转到unauthorizedUrl指定的地址
        return null;
    }

    public void clearAuthenticationInfo() {
        // 清除验证信息
        PrincipalCollection principals = SecurityUtils.getSubject().getPrincipals();
        super.clearCache(principals);
    }
}
