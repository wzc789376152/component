package com.github.wzc789376152.springboot.taskCenter;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.ttl.threadpool.TtlExecutors;
import com.github.wzc789376152.file.service.IFileService;
import com.github.wzc789376152.springboot.config.SpringContextUtil;
import com.github.wzc789376152.springboot.config.init.InitPropertice;
import com.github.wzc789376152.springboot.config.oss.AliyunOssConfig;
import com.github.wzc789376152.springboot.config.oss.AliyunOssService;
import com.github.wzc789376152.springboot.taskCenter.dto.TaskCenterInitDto;
import com.github.wzc789376152.springboot.taskCenter.dto.TaskCenterUpdateDto;
import com.github.wzc789376152.springboot.taskCenter.entity.Taskcenter;
import com.github.wzc789376152.springboot.taskCenter.mapper.TaskcenterMapper;
import com.github.wzc789376152.utils.ClassUtil;
import com.github.wzc789376152.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.*;
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

//    private final TaskcenterMapper taskcenterMapper;

    private final String runUrl;

    private final ITaskCenterManager taskCenterManager;

    public TaskCenterService(T service, String funcName, String callbackFuncName, String runUrl) {
        this.service = service;
        this.funcName = funcName;
        this.method = ClassUtil.getMethod(service.getClass(), funcName);
        this.callbackFuncName = callbackFuncName;
        if (StringUtils.isNotEmpty(callbackFuncName)) {
            this.callbackFunc = ClassUtil.getMethod(service.getClass(), callbackFuncName);
        } else {
            this.callbackFunc = null;
        }
        this.taskCenterManager = SpringContextUtil.getBean(ITaskCenterManager.class);
        this.runUrl = runUrl;
    }

    public TaskCenterService(String serviceName, String funcName, String callbackFuncName, String runUrl) {
        try {
            this.service = (T) SpringContextUtil.getBean(Class.forName(serviceName));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.funcName = funcName;
        this.method = ClassUtil.getMethod(service.getClass(), funcName);
        this.callbackFuncName = callbackFuncName;
        if (StringUtils.isNotEmpty(callbackFuncName)) {
            this.callbackFunc = ClassUtil.getMethod(service.getClass(), callbackFuncName);
        } else {
            this.callbackFunc = null;
        }
        this.taskCenterManager = SpringContextUtil.getBean(ITaskCenterManager.class);
        this.runUrl = runUrl;
    }

    @Override
    public <P> Integer initTask(String title, P param) {
        List<P> list = new ArrayList<>();
        list.add(param);
        return initTask(title, list);
    }

    @Override
    public <P> Integer initTask(String title, List<P> params) {
        log.info("初始化任务:" + title);
        String data = JSONUtils.toJSONString(params);
        String serviceName = service.getClass().getName();
        if (serviceName.contains("$")) {
            serviceName = serviceName.split("\\$")[0];
        }
        JSONObject paramObj = new JSONObject();
        paramObj.put("data", data);
        paramObj.put("name", params.get(0).getClass().getName());
        TaskCenterInitDto taskCenterInitDto = new TaskCenterInitDto();
        taskCenterInitDto.setTitle(title);
        taskCenterInitDto.setServiceName(serviceName);
        taskCenterInitDto.setFuncName(funcName);
        taskCenterInitDto.setCallbackFuncName(callbackFuncName);
        taskCenterInitDto.setServiceParam(paramObj.toJSONString());
        taskCenterInitDto.setRunUrl(runUrl);
        return taskCenterManager.initTask(taskCenterInitDto);
    }

    @Override
    public void runAsync(Integer taskId) {
        Taskcenter taskcenter = taskCenterManager.getTask(taskId);
        if (taskcenter == null) {
            return;
        }
        JSONObject paramObj = JSON.parseObject(taskcenter.getServiceParam());
        String name = paramObj.getString("name");
        String data = paramObj.getString("data");
        List<?> params;
        try {
            Class<?> clazz = Class.forName(name);
            params = JSONArray.parseArray(data, clazz);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        runAsync(taskcenter.getTitle(), taskId, params);
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
        if (params == null || params.isEmpty()) {
            return;
        }
        if (taskId != null) {
            Taskcenter taskcenter = taskCenterManager.getTask(taskId);
            if (taskcenter == null || taskcenter.getStatus() == 1) {
                return;
            }
        } else {
            log.info("初始化任务:" + title);
            taskId = initTask(title, params);
        }
        TaskCenterUpdateDto taskCenterUpdateDto = new TaskCenterUpdateDto();
        taskCenterUpdateDto.setId(taskId);
        taskCenterUpdateDto.setStatus(1);
        taskCenterManager.updateTask(taskCenterUpdateDto);
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
            int total = futureList.isEmpty() ? 1 : futureList.size();
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
                int finalCount = count;
                updateFutureList.add(executorService1.submit(() -> {
                    int process = finalCount * 100 / total;
                    Taskcenter taskcenter1 = taskCenterManager.getTask(taskId);
                    if (taskcenter1.getProgress() < process) {
                        TaskCenterUpdateDto taskCenterUpdateDto = new TaskCenterUpdateDto();
                        taskCenterUpdateDto.setId(taskId);
                        taskCenterUpdateDto.setStatus(1);
                        taskCenterUpdateDto.setProgress(process);
                        taskCenterManager.updateTask(taskCenterUpdateDto);
                    }
                    return true;
                }));
            }
            for (Future<Boolean> future : updateFutureList) {
                future.get();
            }
            String url = "";
            if (!resultList.isEmpty()) {
                //保存excel文件至sso
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ExcelWriter writer = EasyExcel.write(byteArrayOutputStream).build();
                WriteSheet orderSheet = EasyExcel.writerSheet(0, title).head(resultList.get(0).getClass()).build();
                writer.write(resultList, orderSheet);
                writer.finish();
                String fileName = "excel/" + title + ExcelTypeEnum.XLSX.getValue();
                IFileService fileService = SpringContextUtil.getBean(IFileService.class);
                InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                fileService.uploadCache(inputStream, fileName);
                fileService.submit(fileName);
                url = fileService.getDownloadUrl(fileName);
            }
            TaskCenterUpdateDto taskCenterUpdateDto = new TaskCenterUpdateDto();
            taskCenterUpdateDto.setId(taskId);
            taskCenterUpdateDto.setStatus(4);
            taskCenterUpdateDto.setProgress(100);
            taskCenterUpdateDto.setUrl(url);
            taskCenterUpdateDto.setErrorMsg("");
            taskCenterManager.updateTask(taskCenterUpdateDto);
            callbackMap.put("success", true);
            callbackMap.put("url", url);
        } catch (Exception e) {
            log.error("任务失败", e);
            TaskCenterUpdateDto taskCenterUpdateDto = new TaskCenterUpdateDto();
            taskCenterUpdateDto.setId(taskId);
            taskCenterUpdateDto.setErrorMsg(e.getMessage());
            taskCenterUpdateDto.setStatus(3);
            taskCenterManager.updateTask(taskCenterUpdateDto);
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
