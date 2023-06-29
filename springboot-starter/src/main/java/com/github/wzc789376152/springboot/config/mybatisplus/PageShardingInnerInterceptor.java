package com.github.wzc789376152.springboot.config.mybatisplus;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.github.wzc789376152.springboot.annotation.TableFieldType;
import com.github.wzc789376152.springboot.enums.FileType;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.shardingsphere.core.yaml.config.sharding.YamlShardingRuleConfiguration;
import org.apache.shardingsphere.core.yaml.config.sharding.YamlTableRuleConfiguration;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Collections;
import java.util.Map;

public class PageShardingInnerInterceptor implements InnerInterceptor {
    private final YamlShardingRuleConfiguration yamlShardingRuleConfiguration;
    private Map<String, YamlTableRuleConfiguration> shardTableMap;

    public PageShardingInnerInterceptor(YamlShardingRuleConfiguration yamlShardingRuleConfiguration) {
        this.yamlShardingRuleConfiguration = yamlShardingRuleConfiguration;
        getShardingTables();
    }

    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        StatementHandler delegate = (StatementHandler) ReflectUtil.getFieldValue(sh, "delegate");
        // 获取mapper的Statement对象,它描述的是mapper对象的配置
        MappedStatement mappedStatement = (MappedStatement) ReflectUtil.getFieldValue(delegate, "mappedStatement");
        if (SqlCommandType.UPDATE != mappedStatement.getSqlCommandType()) {
            return;
        }
        // 获取数据库对象,此处需要@TableName注解获取表名并进行分表表名匹配
        Class<?> pojoClazz = mappedStatement.getParameterMap().getType();
        if (pojoClazz == null || !pojoClazz.isAnnotationPresent(TableName.class)) {
            return;
        }
        TableName annotation = pojoClazz.getAnnotation(TableName.class);
        BoundSql boundSql = delegate.getBoundSql();
        String sql = boundSql.getSql();
        if (shardTableMap.containsKey(annotation.value())) {
            // 在sql尾部拼接分表所属属性
            YamlTableRuleConfiguration shardConfig = shardTableMap.get(annotation.value());
            String shardingColumn = null;
            if (shardConfig.getTableStrategy().getStandard() != null) {
                shardingColumn = shardConfig.getTableStrategy().getStandard().getShardingColumn();
            }
            if (shardConfig.getTableStrategy().getInline() != null) {
                shardingColumn = shardConfig.getTableStrategy().getInline().getShardingColumn();
            }
            if (shardingColumn != null) {
                sql = sql + " and " + shardingColumn + " = ?";
            }
        }
        for (Field field : pojoClazz.getDeclaredFields()) {
            TableFieldType annotation1 = field.getAnnotation(TableFieldType.class);
            TableField tableField = field.getAnnotation(TableField.class);
            if (annotation1 == null) {
                continue;
            }
            if (annotation1.value().equals(FileType.AutoNumber)) {
                String[] sqlArray = null;
                if (sql.contains("SET")) {
                    sqlArray = sql.split("SET");
                } else {
                    sqlArray = sql.split("set");
                }
                String fileName = StrUtil.toUnderlineCase(field.getName());
                if (tableField != null && StringUtils.isNotEmpty(tableField.value())) {
                    fileName = tableField.value();
                }
                sql = sqlArray[0] + " SET " + fileName + "=" + fileName + "+1," + sqlArray[1];
            }
        }
        ReflectUtil.setFieldValue(boundSql, "sql", sql);
    }

    private void getShardingTables() {
        if (null != shardTableMap) {
            return;
        }
        synchronized (PageShardingInnerInterceptor.class) {
            if (null != shardTableMap) {
                return;
            }
            if (null == yamlShardingRuleConfiguration) {
                shardTableMap = Collections.emptyMap();
                return;
            }
            shardTableMap = yamlShardingRuleConfiguration.getTables();
        }
    }
}
