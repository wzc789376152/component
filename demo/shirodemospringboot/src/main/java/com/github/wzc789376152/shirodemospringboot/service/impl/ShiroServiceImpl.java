package com.github.wzc789376152.shirodemospringboot.service.impl;

import com.github.wzc789376152.shiro.service.IShiroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShiroServiceImpl implements IShiroService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public String findPasswordByUsername(String username) {
        return (String) jdbcTemplate.query("select password from user where username=?", new Object[]{username},
                (ResultSetExtractor<Object>) rs -> {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                    return null;
                });
    }

    @Override
    public String findSaltByUsername(String username) {
        return (String) jdbcTemplate.query("select salt from user where username=?", new Object[]{username},
                (ResultSetExtractor<Object>) rs -> {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                    return null;
                });
    }

    @Override
    public List<String> findRolesByUsername(String username) {
        return jdbcTemplate.queryForList("select r.identifier from role r,user_role ur,user u where r.id = ur.role_id and ur.user_id = u.id and u.username = ?", new Object[]{username}, String.class);
    }

    @Override
    public List<String> findPermissionsByUsername(String username) {
        return jdbcTemplate.queryForList("select p.identifier from permission p,role_permission rp, user_role ur,user u where p.id=rp.permission_id and rp.role_id = ur.role_id  and ur.user_id = u.id and u.username = ?", new Object[]{username}, String.class);
    }
}
