package com.github.wzc789376152.springboot.config.mybatisplus;


import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.github.wzc789376152.springboot.annotation.TableFieldType;
import com.github.wzc789376152.springboot.enums.FileType;
import com.github.wzc789376152.utils.DateUtils;
import com.github.wzc789376152.utils.IpUtil;
import com.github.wzc789376152.utils.JSONUtils;
import com.github.wzc789376152.utils.TokenUtils;
import com.github.wzc789376152.vo.UserInfo;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Mybatis-plus定义新增，修改字段默认值，在新增修改自动插入值
 * 需配合entitu注解@TableField(fill = FieldFill.INSERT)
 * 其中FieldFill.INSERT为新增时设置，FieldFill.INSERT_UPDATE为新增与修改均插入
 */
@Component
public class MetaObjectConfig implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        Class<?> clazz = metaObject.getOriginalObject().getClass();
        for (Field field : clazz.getDeclaredFields()) {
            TableFieldType annotation = field.getAnnotation(TableFieldType.class);
            if (annotation == null) {
                continue;
            }
            Object value = getObject(annotation, field);
            this.strictInsertFill(metaObject, field.getName(), (Class<? super Object>) value.getClass(), value);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Class<?> clazz = metaObject.getOriginalObject().getClass();
        for (Field field : clazz.getDeclaredFields()) {
            TableFieldType annotation = field.getAnnotation(TableFieldType.class);
            if (annotation == null) {
                continue;
            }
            Object value = getObject(annotation, field);
            if (Objects.requireNonNull(annotation.value()) == FileType.AutoNumber) {
                value = field.getName() + "+1";
            }
            this.strictUpdateFill(metaObject, field.getName(), (Class<? super Object>) value.getClass(), value);
        }
    }

    private Object getObject(TableFieldType annotation, Field field) {
        Object value = null;
        UserInfo userInfo = TokenUtils.getCurrentUser();
        switch (annotation.value()) {
            case Ip:
                if (!annotation.defaultValue().equals("")) {
                    value = annotation.defaultValue();
                } else {
                    value = IpUtil.getIpAddr();
                }
                break;
            case DateTime:
                if (!annotation.defaultValue().equals("")) {
                    value = DateUtils.parse(annotation.defaultValue());
                } else {
                    value = DateUtils.now();
                }
                break;
            case Author:
                if (!annotation.defaultValue().equals("")) {
                    value = annotation.defaultValue();
                } else {
                    value = userInfo == null ? "系统" : (StringUtils.isNotEmpty(userInfo.getUserName()) ? userInfo.getUserName() : (ObjectUtils.isNotEmpty(userInfo.getId()) ? userInfo.getId() : "系统"));
                }
                break;
            case AuthorId:
                if (!annotation.defaultValue().equals("")) {
                    value = annotation.defaultValue();
                } else {
                    value = userInfo == null ? 0 : userInfo.getId();
                }
                break;
            case Double:
                value = annotation.defaultValue().equals("") ? 0d : Double.parseDouble(annotation.defaultValue());
                break;
            case String:
                value = annotation.defaultValue().equals("") ? "" : annotation.defaultValue();
                break;
            case Boolean:
                value = !annotation.defaultValue().equals("") && Boolean.getBoolean(annotation.defaultValue());
                break;
            case Integer:
                value = annotation.defaultValue().equals("") ? 0 : Integer.parseInt(annotation.defaultValue());
                break;
        }
        if (value == null) {
            return value;
        }
        String typeName = field.getType().getName();
        try {
            if (!typeName.equals(value.getClass().getName())) {
                if (typeName.equals(Long.class.getName())) {
                    return Long.parseLong(value.toString());
                }
                if (typeName.equals(Integer.class.getName())) {
                    return Integer.parseInt(value.toString());
                }
                if (typeName.equals(String.class.getName())) {
                    return JSONUtils.parse(value,String.class);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(field.getName() + "参数类型不符");
        }
        if (!typeName.equals(value.getClass().getName())) {
            throw new RuntimeException(field.getName() + "参数类型不符");
        }
        return value;
    }
}

