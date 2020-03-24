package com.github.wzc789376152.shiro.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class ShiroInitDataRunner implements ApplicationRunner {
    Logger logger = LoggerFactory.getLogger(ShiroInitDataRunner.class);
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void run(ApplicationArguments args) throws Exception {
        try {
            logger.info("初始化shiro数据库");
            DataSource dataSource = jdbcTemplate.getDataSource();
            String driverType = dataSource.getConnection().getMetaData().getDatabaseProductName();
            ClassPathResource recordsSys = null;
            if (driverType.equals("MySQL")) {
                recordsSys = new ClassPathResource("create_shiro_mysql.sql");
            }
            DataSourceInitializer dsi = new DataSourceInitializer();
            dsi.setDataSource(dataSource);
            dsi.setDatabasePopulator(new ResourceDatabasePopulator(true, true, "utf-8", recordsSys));
            dsi.setEnabled(true);
            dsi.afterPropertiesSet();
        } catch (Exception e) {
            logger.warn("表已存在");
        }
    }
}
