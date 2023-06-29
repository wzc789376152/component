package com.github.wzc789376152.springboot.annotation;



import com.github.wzc789376152.springboot.enums.FileType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author daiyiming 2020/12/8 9:33 上午
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TableFieldType {
    FileType value();

    String defaultValue() default "";
}
