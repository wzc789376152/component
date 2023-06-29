package com.github.wzc789376152.springboot.enums;

import java.util.Date;

public enum FileType {
    DateTime(Date.class),
    Ip(java.lang.String.class),
    Author(java.lang.String.class),
    AuthorId(java.lang.Integer.class),
    Delete(java.lang.Boolean.class),
    String(java.lang.String.class),
    Double(java.lang.Double.class),
    Integer(java.lang.Integer.class),
    Boolean(java.lang.Boolean.class),
    AutoNumber(java.lang.Integer.class);

    FileType(Class<?> clazz) {
        this.clazz = clazz;
    }

    private final Class<?> clazz;

    public Class<?> getClazz() {
        return clazz;
    }
}
