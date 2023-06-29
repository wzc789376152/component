package com.github.wzc789376152.annotation;

import com.alibaba.excel.enums.CellExtraTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelExtra {
    String value();

    CellExtraTypeEnum type() default CellExtraTypeEnum.MERGE;
}
