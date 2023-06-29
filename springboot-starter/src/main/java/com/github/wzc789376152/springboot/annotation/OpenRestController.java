package com.github.wzc789376152.springboot.annotation;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@RestController
@Documented
@Component
public @interface OpenRestController {
}
