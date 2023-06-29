package com.github.wzc789376152.springboot.config.init;


import com.github.wzc789376152.springboot.config.shardingsphere.ShardingPropertics;
import com.github.wzc789376152.springboot.config.taskCenter.TaskCenterProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.core.yaml.config.sharding.YamlShardingRuleConfiguration;
import org.apache.shardingsphere.core.yaml.config.sharding.YamlTableRuleConfiguration;
import org.apache.shardingsphere.underlying.common.config.inline.InlineExpressionParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.Comparator;
import java.util.List;


@Slf4j
@Configuration
public class InitPlatformConfig extends InitConfig {
    @Autowired(required = false)
    InitService initService;
    @Autowired(required = false)
    @Lazy
    private DataSource dataSource;
    @Autowired(required = false)
    private TaskCenterProperties taskCenterProperties;
    @Autowired(required = false)
    private ShardingPropertics shardingPropertics;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired(required = false)
    private YamlShardingRuleConfiguration yamlShardingRuleConfiguration;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (initService != null) {
            initService.init();
        }
        initDb();
        super.run(args);
    }

    private void initDb() {
        if (taskCenterProperties != null && taskCenterProperties.getEnable() && taskCenterProperties.getInitTable()) {
            Resource resources = new ClassPathResource("sql/db_taskcenter.sql");
            ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
            resourceDatabasePopulator.addScripts(resources);
            if (dataSource != null) {
                resourceDatabasePopulator.execute(dataSource);
            }
        }
        if (shardingPropertics != null && shardingPropertics.getInitTable() && yamlShardingRuleConfiguration != null && yamlShardingRuleConfiguration.getTables() != null && yamlShardingRuleConfiguration.getTables().size() > 0) {
            for (YamlTableRuleConfiguration yamlTableRuleConfiguration : yamlShardingRuleConfiguration.getTables().values()) {
                InlineExpressionParser inlineExpressionParser = new InlineExpressionParser(yamlTableRuleConfiguration.getActualDataNodes());
                List<String> tableList = inlineExpressionParser.splitAndEvaluate();
                tableList.sort(Comparator.comparing(i -> i));
                if (tableList.size() > 1) {
                    for (int i = 1; i < tableList.size(); i++) {
                        String lastTableName = getTableName(tableList.get(i - 1));
                        String tableName = getTableName(tableList.get(i));
                        try {
                            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + tableName + " LIKE " + lastTableName + ";");
                        } catch (Exception e) {
                            log.warn("表创建失败,{}", e.getMessage());
                        }
                    }
                }
            }
        }
    }

    private String getTableName(String tableName) {
        String[] array = tableName.split("\\.");
        if (array.length == 2) {
            return array[1];
        }
        return tableName;
    }
}
