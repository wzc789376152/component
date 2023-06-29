package com.github.wzc789376152.springboot.config.taskCenter;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.wzc789376152.springboot.taskCenter.entity.Taskcenter;
import com.github.wzc789376152.springboot.taskCenter.mapper.TaskcenterMapper;
import com.github.wzc789376152.springboot.utils.TaskCenterUtils;
import com.github.wzc789376152.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Slf4j
@Configuration
@MapperScan(basePackages = "com.github.wzc789376152.springboot.taskCenter.mapper")
@EnableConfigurationProperties(TaskCenterProperties.class)
@ConditionalOnProperty(prefix = "wzc.task-center", name = "enable", havingValue = "true")
public class TaskCenterConfig {
    @Resource
    @Lazy
    private TaskcenterMapper taskcenterMapper;

    @Scheduled(cron = "0 0/1 * * * ?")
    public void redoTask() {
        List<Taskcenter> taskcenterList = taskcenterMapper.selectList(Wrappers.<Taskcenter>lambdaQuery()
                .eq(Taskcenter::getStatus, 2)
                .orderByAsc(Taskcenter::getGmtCreated));
        if (taskcenterList.size() > 0) {
            log.info("开始重试暂停中任务");
            for (Taskcenter taskcenter : taskcenterList) {
                TaskCenterUtils.redo(taskcenter.getId());
            }
            log.info("重试暂停中任务完成");
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void removeTask() {
        List<Taskcenter> taskcenterList = taskcenterMapper.selectList(Wrappers.<Taskcenter>lambdaQuery()
                .le(Taskcenter::getGmtCreated, DateUtils.addDateMonths(new Date(), -2)));
        if (taskcenterList.size() > 0) {
            log.info("开始删除2个月前任务");
            for (Taskcenter taskcenter : taskcenterList) {
                TaskCenterUtils.remove(taskcenter.getId());
            }
            log.info("删除2个月前任务完成");
        }
    }
}
