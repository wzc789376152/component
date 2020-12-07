package com.github.wzc789376152.shiro.realm;

import com.github.wzc789376152.shiro.token.JwtToken;
import com.github.wzc789376152.shiro.token.UsernameCodeToken;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.realm.Realm;

import java.util.ArrayList;
import java.util.Collection;

public class UserModularRealmAuthenticator extends ModularRealmAuthenticator {


    @Override
    protected AuthenticationInfo doAuthenticate(AuthenticationToken authenticationToken) throws AuthenticationException {
        assertRealmsConfigured();
        // 所有Realm
        Collection<Realm> realms = getRealms();
        // 登录类型对应的所有Realm
        Collection<Realm> typeRealms = new ArrayList<>();
        // 强制转换回自定义的Token
        for (Realm realm : realms) {
            if (realm.getName().contains("Jwt")) {
                if (authenticationToken instanceof JwtToken) {
                    typeRealms.add(realm);
                    break;
                }
            } else if (realm.getName().contains("Code")) {
                if (authenticationToken instanceof UsernameCodeToken) {
                    typeRealms.add(realm);
                    break;
                }
            } else {
                typeRealms.add(realm);
            }
        }
        // 判断是单Realm还是多Realm
        if (typeRealms.size() == 1) {
            return doSingleRealmAuthentication(typeRealms.iterator().next(), authenticationToken);
        } else {
            return doMultiRealmAuthentication(typeRealms, authenticationToken);
        }
    }
}
