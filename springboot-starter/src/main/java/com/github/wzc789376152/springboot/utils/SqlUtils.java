package com.github.wzc789376152.springboot.utils;

import com.alibaba.ttl.TransmittableThreadLocal;

public class SqlUtils {
    private static final TransmittableThreadLocal<SqlUtils> SQL = TransmittableThreadLocal.withInitial(SqlUtils::new);

    private String sql;

    private String params;

    private SqlUtils() {
    }

    public static SqlUtils getInstance() {
        return SQL.get();
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public void reset() {
        sql = null;
        SQL.remove();
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
}
