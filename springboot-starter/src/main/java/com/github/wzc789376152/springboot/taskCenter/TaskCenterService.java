package com.github.wzc789376152.springboot.taskCenter;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.ttl.threadpool.TtlExecutors;
import com.github.wzc789376152.springboot.config.SpringContextUtil;
import com.github.wzc789376152.springboot.config.init.InitPropertice;
import com.github.wzc789376152.springboot.config.oss.AliyunOssService;
import com.github.wzc789376152.springboot.taskCenter.entity.Taskcenter;
import com.github.wzc789376152.springboot.taskCenter.mapper.TaskcenterMapper;
import com.github.wzc789376152.utils.ClassUtil;
import com.github.wzc789376152.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TaskCenterService<T> implements ITaskCenterService<T> {
    private final T service;

    private final String funcName;
    private final Method method;

    private final String callbackFuncName;

    private final Method callbackFunc;

    private final TaskcenterMapper taskcenterMapper;

    public TaskCenterService(T service, String funcName, String callbackFuncName) {
        this.service = service;
        this.funcName = funcName;
        this.method = ClassUtil.getMethod(service.getClass(), funcName);
        this.callbackFuncName = callbackFuncName;
        if (StringUtils.isNotEmpty(callbackFuncName)) {
            this.callbackFunc = ClassUtil.getMethod(service.getClass(), callbackFuncName);
        } else {
            this.callbackFunc = null;
        }
        this.taskcenterMapper = SpringContextUtil.getBean(TaskcenterMapper.class);
    }

    public TaskCenterService(String serviceName, String funcName, String callbackFuncName) {
        this.service = (T) SpringContextUtil.getBean(serviceName.substring(0, 1).toLowerCase() + serviceName.substring(1));
        this.funcName = funcName;
        this.method = ClassUtil.getMethod(service.getClass(), funcName);
        this.callbackFuncName = callbackFuncName;
        if (StringUtils.isNotEmpty(callbackFuncName)) {
            this.callbackFunc = ClassUtil.getMethod(service.getClass(), callbackFuncName);
        } else {
            this.callbackFunc = null;
        }
        this.taskcenterMapper = SpringContextUtil.getBean(TaskcenterMapper.class);
    }

    @Override
    public <P> void runAsync(String title, P param) {
        runAsync(title, null, param);
    }

    @Override
    public <P> void runAsync(String title, List<P> params) {
        runAsync(title, null, params);
    }

    @Override
    public <P> void runAsync(String title, Integer taskId, P param) {
        if (param == null) {
            return;
        }
        List<P> list = new ArrayList<>();
        list.add(param);
        runAsync(title, taskId, list);
    }

    @Override
    public <P> void runAsync(String title, Integer taskId, List<P> params) {
        if (params == null || params.size() == 0) {
            return;
        }
        String data = JSONUtils.toJSONString(params);
        if (taskId == null) {//初始化任务
            log.info("初始化任务:" + title);
            Taskcenter taskcenter = new Taskcenter();
            taskcenter.setTitle(title);
            String serviceName = service.getClass().getSimpleName();
            if (serviceName.contains("$")) {
                serviceName = serviceName.split("\\$")[0];
            }
            taskcenter.setServiceName(serviceName);
            taskcenter.setServiceMethod(funcName);
            taskcenter.setCallbackServiceMethod(callbackFuncName);
            JSONObject paramObj = new JSONObject();
            paramObj.put("data", data);
            paramObj.put("name", params.get(0).getClass().getName());
            taskcenter.setServiceParam(paramObj.toJSONString());
            taskcenter.setProgress(0);
            taskcenter.setStatus(1);
            taskcenterMapper.insert(taskcenter);
            taskId = taskcenter.getId();
        } else {
            Taskcenter taskcenter = taskcenterMapper.selectById(taskId);
            if (taskcenter == null || taskcenter.getStatus() == 1) {
                return;
            }
        }
        Taskcenter taskcenter1 = new Taskcenter();
        taskcenter1.setId(taskId);
        taskcenter1.setStatus(1);
        taskcenter1.setProgress(0);
        taskcenterMapper.updateById(taskcenter1);
        ExecutorService executorService = SpringContextUtil.getBean("taskCenterAsync", ExecutorService.class);
        Integer finalTaskId = taskId;
        executorService.submit(() -> run(title, finalTaskId, params.toArray()));
    }

    private <P> void run(String title, Integer taskId, P... params) {
        RedissonClient redissonClient = SpringContextUtil.getBean(RedissonClient.class);
        RBucket<Object> rBucket = redissonClient.getBucket("redisson:taskCenter:" + title + ":" + taskId);
        boolean isLock = rBucket.trySet(1, 3600L, TimeUnit.SECONDS);
        if (!isLock) {//等待下次执行
            return;
        }
        log.info("开始执行任务:" + title);
        Map<String, Object> callbackMap = new HashMap<>();
        callbackMap.put("taskId", taskId);
        callbackMap.put("data", JSONUtils.toJSONString(params));
        callbackMap.put("startTime", System.currentTimeMillis());
        try {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(16);
            executor.setMaxPoolSize(32);
            executor.setQueueCapacity(100000);
            executor.setThreadNamePrefix("taskCenterItem-handle-");
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            executor.initialize();
            ExecutorService executorService = TtlExecutors.getTtlExecutorService(executor.getThreadPoolExecutor());
            List<Future<Object>> futureList = new ArrayList<>();
            for (P param : params) {
                futureList.add(executorService.submit(() -> method.invoke(service, param)));
            }
            List<Object> resultList = new ArrayList<>();
            int total = futureList.size() == 0 ? 1 : futureList.size();
            int count = 0;
            ThreadPoolTaskExecutor executor1 = new ThreadPoolTaskExecutor();
            executor1.setCorePoolSize(16);
            executor1.setMaxPoolSize(32);
            executor1.setQueueCapacity(100000);
            executor1.setThreadNamePrefix("taskCenterItemUpdate-handle-");
            executor1.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            executor1.initialize();
            ExecutorService executorService1 = TtlExecutors.getTtlExecutorService(executor1.getThreadPoolExecutor());
            List<Future<Boolean>> updateFutureList = new ArrayList<>();
            for (Future<Object> future : futureList) {
                Object result = future.get();
                count++;
                if (result instanceof List) {
                    resultList.addAll((List) result);
                }
                Integer finalTaskId = taskId;
                int finalCount = count;
                updateFutureList.add(executorService1.submit(() -> {
                    int process = finalCount * 100 / total;
                    Taskcenter taskcenter1 = taskcenterMapper.selectById(finalTaskId);
                    if (taskcenter1.getProgress() < process) {
                        Taskcenter updateCenter = new Taskcenter();
                        updateCenter.setId(finalTaskId);
                        updateCenter.setStatus(1);
                        updateCenter.setProgress(process);
                        taskcenterMapper.updateById(updateCenter);
                    }
                    return true;
                }));
            }
            for (Future<Boolean> future : updateFutureList) {
                future.get();
            }
            String url = "";
            if (resultList.size() > 0) {
                //保存excel文件至sso
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ExcelWriter writer = EasyExcel.write(byteArrayOutputStream).build();
                WriteSheet orderSheet = EasyExcel.writerSheet(0, title).head(resultList.get(0).getClass()).build();
                writer.write(resultList, orderSheet);
                writer.finish();
                AliyunOssService aliyunOssService = SpringContextUtil.getBean(AliyunOssService.class);
                if (aliyunOssService != null) {
                    url = aliyunOssService.upload(byteArrayOutputStream.toByteArray(), title + ExcelTypeEnum.XLSX.getValue());
                    if (!url.startsWith("http")) {
                        url = "https://" + url;
                    }
                } else {
                    File file = new File("excel/" + title + ExcelTypeEnum.XLSX.getValue());
                    if (!file.getParentFile().exists()) {
                        file.mkdirs();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(byteArrayOutputStream.toByteArray());
                    url = "excel/" + title + ExcelTypeEnum.XLSX.getValue();
                }
            }
            Taskcenter taskcenter1 = new Taskcenter();
            taskcenter1.setId(taskId);
            taskcenter1.setStatus(4);
            taskcenter1.setProgress(100);
            taskcenter1.setFinishTime(new Date());
            taskcenter1.setUrl(url);
            taskcenter1.setErrorMsg("");
            taskcenterMapper.updateById(taskcenter1);
            callbackMap.put("success", true);
            callbackMap.put("url", url);
        } catch (Exception e) {
            Taskcenter taskcenter = new Taskcenter();
            taskcenter.setId(taskId);
            taskcenter.setErrorMsg(e.getMessage());
            taskcenter.setStatus(3);
            taskcenterMapper.updateById(taskcenter);
            callbackMap.put("success", false);
            callbackMap.put("error", e.getMessage());
        } finally {
            rBucket.delete();
        }
        log.info("任务结束:" + title);
        callbackMap.put("endTime", System.currentTimeMillis());
        if (this.callbackFunc != null) {
            try {
                callbackFunc.invoke(service, callbackMap);
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("回调错误", e);
            }
        }
    }
}
