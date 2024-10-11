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
import com.github.wzc789376152.springboot.config.redis.IRedisService;
import com.github.wzc789376152.springboot.taskCenter.dto.TaskCenterInitDto;
import com.github.wzc789376152.springboot.taskCenter.dto.TaskCenterUpdateDto;
import com.github.wzc789376152.springboot.taskCenter.entity.Taskcenter;
import com.github.wzc789376152.springboot.taskCenter.mapper.TaskcenterMapper;
import com.github.wzc789376152.springboot.utils.CurrentHashMapUtil;
import com.github.wzc789376152.springboot.utils.TaskCenterUtils;
import com.github.wzc789376152.utils.ClassUtil;
import com.github.wzc789376152.utils.JSONUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class TaskCenterService implements ITaskCenterService {
    private final Object service;

    private final String funcName;
    private final Method method;

    private final String callbackFuncName;

    private final Method callbackFunc;

    private final String runUrl;

    private final ITaskCenterManager taskCenterManager;

    public TaskCenterService(Object service, String funcName, String callbackFuncName, String runUrl) {
        this.service = service;
        this.funcName = funcName;
        this.method = ClassUtil.getMethod(service.getClass(), funcName);
        if (this.method == null) {
            throw new RuntimeException("方法初始化失败");
        }
        this.callbackFuncName = callbackFuncName;
        if (StringUtils.isNotEmpty(callbackFuncName)) {
            this.callbackFunc = ClassUtil.getMethod(service.getClass(), callbackFuncName);
        } else {
            this.callbackFunc = null;
        }
        this.taskCenterManager = SpringContextUtil.getBean(ITaskCenterManager.class);
        this.runUrl = runUrl;
    }

    public TaskCenterService(String serviceName, String funcName, String callbackFuncName, String runUrl) throws ClassNotFoundException {
        this(SpringContextUtil.getBean(Class.forName(serviceName)), funcName, callbackFuncName, runUrl);
    }

    @Override
    public <P> Integer initTask(Integer id, String title, List<P> params) {
        log.info("初始化任务:" + title);
        String data = JSONUtils.toJSONString(params);
        String serviceName = service.getClass().getName();
        if (serviceName.contains("$")) {
            serviceName = serviceName.split("\\$")[0];
        }
        JSONObject paramObj = new JSONObject();
        paramObj.put("data", data);
        paramObj.put("name", params.get(0).getClass().getName());
        if (id != null) {
            Taskcenter taskcenter = taskCenterManager.getTask(id);
            if (taskcenter == null) {
                id = null;
            }
        }
        if (id == null) {
            TaskCenterInitDto taskCenterInitDto = new TaskCenterInitDto();
            taskCenterInitDto.setTitle(title);
            taskCenterInitDto.setServiceName(serviceName);
            taskCenterInitDto.setFuncName(funcName);
            taskCenterInitDto.setCallbackFuncName(callbackFuncName);
            taskCenterInitDto.setRunUrl(runUrl);
            id = taskCenterManager.initTask(taskCenterInitDto);
        }
        IRedisService redisService = SpringContextUtil.getBean(IRedisService.class);
        InitPropertice initPropertice = SpringContextUtil.getBean(InitPropertice.class);
        String key = "taskCenter:" + initPropertice.getServerName() + ":serviceParam:" + id;
        redisService.setCacheObject(key, paramObj, 7L, TimeUnit.DAYS);
        CurrentHashMapUtil.put(key, paramObj, 1L, TimeUnit.DAYS);
        return id;
    }

    @Override
    public <P> Integer initTask(Integer id, String title, P param) {
        return initTask(id, title, Lists.newArrayList(param));
    }

    @Override
    public <P> Integer initTask(String title, List<P> param) {
        return initTask(null, title, param);
    }

    @Override
    public <P> Integer initTask(String title, P param) {
        return initTask(null, title, Lists.newArrayList(param));
    }

    @Override
    public void runAsync(Integer taskId) {
        Taskcenter taskcenter = taskCenterManager.getTask(taskId);
        if (taskcenter == null) {
            return;
        }
        JSONObject paramObj = TaskCenterUtils.getParam(taskId);
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
        taskCenterUpdateDto.setProgress(0);
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
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(100000);
        executor.setThreadNamePrefix("taskCenterItem-handle-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        ExecutorService taskItemExecutor = TtlExecutors.getTtlExecutorService(executor.getThreadPoolExecutor());

        ThreadPoolTaskExecutor executor1 = new ThreadPoolTaskExecutor();
        executor1.setCorePoolSize(1);
        executor1.setMaxPoolSize(1);
        executor1.setQueueCapacity(100);
        executor1.setThreadNamePrefix("taskCenterUpdate-handle-");
        executor1.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor1.initialize();
        ExecutorService taskUpdateExecutor = TtlExecutors.getTtlExecutorService(executor1.getThreadPoolExecutor());
        try {
            List<Future<Object>> futureList = new ArrayList<>();
            for (P param : params) {
                futureList.add(taskItemExecutor.submit(() -> method.invoke(service, param)));
            }
            List<Object> resultList = new ArrayList<>();
            int total = futureList.isEmpty() ? 1 : futureList.size();
            int count = 0;
//            ThreadPoolTaskExecutor executor1 = new ThreadPoolTaskExecutor();
//            executor1.setCorePoolSize(16);
//            executor1.setMaxPoolSize(32);
//            executor1.setQueueCapacity(100000);
//            executor1.setThreadNamePrefix("taskCenterItemUpdate-handle-");
//            executor1.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
//            executor1.initialize();
//            ExecutorService executorService1 = TtlExecutors.getTtlExecutorService(executor1.getThreadPoolExecutor());
//            List<Future<Boolean>> updateFutureList = new ArrayList<>();
            for (Future<Object> future : futureList) {
                Object result = future.get();
                count++;
                if (result instanceof List) {
                    resultList.addAll((List) result);
                }
                int process = count * 100 / (total * 2);
                Future future1 = taskUpdateExecutor.submit(() -> {
                    Taskcenter taskcenter1 = taskCenterManager.getTask(taskId);
                    if (taskcenter1.getProgress() < process) {
                        TaskCenterUpdateDto taskCenterUpdateDto = new TaskCenterUpdateDto();
                        taskCenterUpdateDto.setId(taskId);
                        taskCenterUpdateDto.setStatus(1);
                        taskCenterUpdateDto.setProgress(process);
                        taskCenterManager.updateTask(taskCenterUpdateDto);
                    }
                });
                future1.get();
            }
//            for (Future<Boolean> future : updateFutureList) {
//                future.get();
//            }
            String url = "";
            if (!resultList.isEmpty()) {
                List<List<Object>> splitList = splitList(resultList, 1000000);
                String filePath = System.getProperty("user.dir") + "/excel";
                File file = new File(filePath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                filePath = filePath + "/" + UUID.randomUUID() + ExcelTypeEnum.XLSX.getValue();
                file = new File(filePath);
                OutputStream outputStream = Files.newOutputStream(file.toPath());
                //保存excel文件至sso
                ExcelWriter writer = EasyExcel.write(outputStream).excelType(ExcelTypeEnum.XLSX).build();
                for (int i = 0; i < splitList.size(); i++) {
                    for (List<Object> list : splitList(splitList.get(i), 10000)) {
                        WriteSheet orderSheet = EasyExcel.writerSheet(i, "sheet" + (i + 1)).head(resultList.get(0).getClass()).build();
                        writer.write(list, orderSheet);
                    }
                }
                writer.finish();
                String fileName = "excel/" + UUID.randomUUID().toString().replace("-", "") + "---" + title + ExcelTypeEnum.XLSX.getValue();
                IFileService fileService = SpringContextUtil.getBean(IFileService.class);
                InputStream inputStream = Files.newInputStream(file.toPath());
                fileService.uploadCache(inputStream, fileName);
                fileService.submit(fileName);
                url = fileService.getDownloadUrl(fileName);
                try {
                    Files.deleteIfExists(file.toPath());
                } catch (Exception e) {
                }
            }
            TaskCenterUpdateDto taskCenterUpdateDto = new TaskCenterUpdateDto();
            taskCenterUpdateDto.setId(taskId);
            taskCenterUpdateDto.setStatus(4);
            taskCenterUpdateDto.setProgress(100);
            taskCenterUpdateDto.setUrl(url);
            taskCenterUpdateDto.setErrorMsg("");
            Future future1 = taskUpdateExecutor.submit(() -> taskCenterManager.updateTask(taskCenterUpdateDto));
            future1.get();
            callbackMap.put("success", true);
            callbackMap.put("url", url);
        } catch (Exception e) {
            log.error("任务失败", e);
            TaskCenterUpdateDto taskCenterUpdateDto = new TaskCenterUpdateDto();
            taskCenterUpdateDto.setId(taskId);
            taskCenterUpdateDto.setErrorMsg(e.getMessage());
            taskCenterUpdateDto.setStatus(3);
            Future future1 = taskUpdateExecutor.submit(() -> taskCenterManager.updateTask(taskCenterUpdateDto));
            try {
                future1.get();
            } catch (InterruptedException | ExecutionException ex) {
            }
            callbackMap.put("success", false);
            callbackMap.put("error", e.getMessage());
        } finally {
            taskItemExecutor.shutdown();
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

    /**
     * 将大 List 拆分成小 List，每个小 List 包含指定数量的元素
     *
     * @param sourceList 源大 List
     * @param chunkSize  每个小 List 的大小
     * @return 拆分后的 List 的 List
     */
    private List<List<Object>> splitList(List<Object> sourceList, int chunkSize) {
        List<List<Object>> result = new ArrayList<>();

        for (int i = 0; i < sourceList.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, sourceList.size());
            result.add(new ArrayList<>(sourceList.subList(i, end)));
        }

        return result;
    }

}
