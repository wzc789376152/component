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
import org.apache.shardingsphere.sharding.yaml.config.YamlShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.yaml.config.rule.YamlTableRuleConfiguration;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.*;

public class PageShardingInnerInterceptor implements InnerInterceptor {
    private final YamlShardingRuleConfiguration yamlShardingRuleConfiguration;
    private Map<String, YamlTableRuleConfiguration> shardTableMap;

    public PageShardingInnerInterceptor(YamlShardingRuleConfiguration yamlShardingRuleConfiguration) {
        this.yamlShardingRuleConfiguration = yamlShardingRuleConfiguration;
        getShardingTables();
    }

    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        StatementHandler delegate = (StatementHandler) ReflectUtil.getFieldValue(sh, "delegate");

        // 获取 MappedStatement（MyBatis 映射 SQL 配置）
        MappedStatement mappedStatement = (MappedStatement) ReflectUtil.getFieldValue(delegate, "mappedStatement");
        if (mappedStatement == null || SqlCommandType.UPDATE != mappedStatement.getSqlCommandType()) {
            return;
        }

        // 获取数据库实体类（需要 @TableName 注解）
        Class<?> pojoClazz = mappedStatement.getParameterMap().getType();
        if (pojoClazz == null || !pojoClazz.isAnnotationPresent(TableName.class)) {
            return;
        }

        // 获取表名
        TableName tableNameAnnotation = pojoClazz.getAnnotation(TableName.class);
        String tableName = tableNameAnnotation.value();

        BoundSql boundSql = delegate.getBoundSql();
        String originalSql = boundSql.getSql();
        StringBuilder sql = new StringBuilder(originalSql);

        // **处理分表逻辑**
        if (shardTableMap.containsKey(tableName)) {
            YamlTableRuleConfiguration shardConfig = shardTableMap.get(tableName);
            List<String> shardingColumns = new ArrayList<>();

            // **安全地获取分片字段**
            if (shardConfig.getTableStrategy() != null) {
                if (shardConfig.getTableStrategy().getStandard() != null) {
                    shardingColumns.add(shardConfig.getTableStrategy().getStandard().getShardingColumn());
                }
                if (shardConfig.getTableStrategy().getComplex() != null) {
                    shardingColumns.addAll(Arrays.asList(shardConfig.getTableStrategy().getComplex().getShardingColumns().split(",")));
                }
            }

            for (String shardingColumn : shardingColumns) {
                if (StringUtils.isNotBlank(shardingColumn)) {
                    sql.append(" AND ").append(shardingColumn).append(" = ?");
                }
            }
        }

        // **处理 @TableFieldType.AutoNumber 逻辑**
        for (Field field : pojoClazz.getDeclaredFields()) {
            TableFieldType fieldTypeAnnotation = field.getAnnotation(TableFieldType.class);
            TableField tableFieldAnnotation = field.getAnnotation(TableField.class);

            if (fieldTypeAnnotation != null && fieldTypeAnnotation.value().equals(FileType.AutoNumber)) {
                // 获取 SQL 结构（确保包含 SET 关键字）
                String[] sqlParts = sql.toString().split("(?i)\\bSET\\b", 2);
                if (sqlParts.length < 2) {
                    continue;
                }

                // **字段名转换**（驼峰转下划线）
                String columnName = StrUtil.toUnderlineCase(field.getName());
                if (tableFieldAnnotation != null && StringUtils.isNotBlank(tableFieldAnnotation.value())) {
                    columnName = tableFieldAnnotation.value();
                }

                // **拼接自增字段 SQL**
                sql = new StringBuilder(sqlParts[0])
                        .append(" SET ")
                        .append(columnName).append(" = ").append(columnName).append(" + 1, ")
                        .append(sqlParts[1]);
            }
        }

        // **设置修改后的 SQL**
        ReflectUtil.setFieldValue(boundSql, "sql", sql.toString());
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
