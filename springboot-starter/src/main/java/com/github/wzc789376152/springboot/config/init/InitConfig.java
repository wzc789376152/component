package com.github.wzc789376152.springboot.config.init;


import com.github.wzc789376152.springboot.config.swagger.SwaggerPropertics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;


@Slf4j
public class InitConfig implements ApplicationRunner {
    @Autowired(required = false)
    SwaggerPropertics swaggerPropertics;

    @Autowired
    InitPropertice initPropertice;

    static {
        log.info(">>>>>>>>>>>>>>>>>>>> 系统正在启动中......");
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info(">>>>>>>>>>>>>>>>>>>> 初始化数据完成 >>>>>>>>>>>>>>>>>>>>");
        log.info("应用名: " + initPropertice.getServerName());
        log.info("端口号: " + initPropertice.getServerPort());
        log.info("运行环境: " + initPropertice.getProfilesActive());
        if (swaggerPropertics != null && swaggerPropertics.getEnable()) {
            log.info("swagger地址: http://127.0.0.1:" + initPropertice.getServerPort() + "/doc.html");
        }
        log.info(">>>>>>>>>>>>>>>>>>>> " + initPropertice.getServerName() + "项目启动成功 >>>>>>>>>>>>>>>>>>>>");
    }
}
