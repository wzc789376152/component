package com.github.wzc789376152.shirodemospringboot;

import com.github.wzc789376152.shiro.utils.CipherKeyUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ShirodemospringbootApplicationTests {

    @Test
    void contextLoads() {
        System.out.println(CipherKeyUtils.createCipherKey());
    }

}
