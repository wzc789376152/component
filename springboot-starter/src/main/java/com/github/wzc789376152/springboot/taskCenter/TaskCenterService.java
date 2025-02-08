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
        // 获取分布式锁，避免重复执行
        RedissonClient redissonClient = SpringContextUtil.getBean(RedissonClient.class);
        String lockKey = "redisson:taskCenter:" + title + ":" + taskId;
        RBucket<Object> lockBucket = redissonClient.getBucket(lockKey);
        if (!lockBucket.trySet(1, 3600L, TimeUnit.SECONDS)) {
            return; // 未获得锁，退出
        }

        log.info("开始执行任务: {}", title);
        Map<String, Object> callbackMap = new HashMap<>();
        callbackMap.put("taskId", taskId);
        callbackMap.put("data", JSONUtils.toJSONString(params));
        callbackMap.put("startTime", System.currentTimeMillis());

        // 创建两个线程池：任务执行和任务进度更新
        ExecutorService taskExecutor = createExecutorService(16, 32, 100000, "taskCenterItem-handle-");
        ExecutorService progressExecutor = createExecutorService(1, 1, 100, "taskCenterUpdate-handle-");

        // Excel 相关变量
        boolean isExcelGenerated = false;
        ExcelWriter excelWriter = null;
        BufferedOutputStream bufferedOutputStream = null;
        File excelFile = null;
        int sheetIndex = 0;
        int excelRowCount = 0;

        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (P param : params) {
                futures.add(taskExecutor.submit(() -> method.invoke(service, param)));
            }
            int totalTasks = Math.max(futures.size(), 1);
            int completedTasks = 0;

            // 遍历各个任务执行结果
            for (Future<Object> future : futures) {
                Object result = future.get();
                completedTasks++;

                // 如果返回结果为 List，则写入 Excel
                if (result instanceof List) {
                    List<?> resultList = (List<?>) result;
                    if (!resultList.isEmpty()) {
                        // 延迟初始化 ExcelWriter（只在首次写入数据时初始化）
                        if (!isExcelGenerated) {
                            isExcelGenerated = true;
                            excelFile = createExcelFile();
                            bufferedOutputStream = new BufferedOutputStream(Files.newOutputStream(excelFile.toPath()));
                            excelWriter = EasyExcel.write(bufferedOutputStream)
                                    .excelType(ExcelTypeEnum.XLSX)
                                    .build();
                        }
                        // 按每 10000 条记录分批写入，超过 100 万条记录换新 sheet
                        for (List<Object> batch : splitList((List<Object>) resultList, 10000)) {
                            if (excelWriter != null) {
                                if (excelRowCount >= 1_000_000) {
                                    excelRowCount = 0;
                                    sheetIndex++;
                                }
                                WriteSheet sheet = EasyExcel.writerSheet(sheetIndex, "sheet" + (sheetIndex + 1))
                                        .head(resultList.get(0).getClass())
                                        .build();
                                excelWriter.write(batch, sheet);
                                excelRowCount += batch.size();
                            }
                        }
                    }
                }
                // 异步更新进度（任务项完成占总进度的一半）
                final int progress = completedTasks * 90 / totalTasks;
                progressExecutor.submit(() -> updateTaskProgress(taskId, progress));
            }

            String url = "";
            // 如果有 Excel 数据，结束写入并上传文件
            if (isExcelGenerated && excelWriter != null) {
                excelWriter.finish();
                url = uploadExcelFile(excelFile, title);
            }
            updateTaskCompletion(taskId, url);
            callbackMap.put("success", true);
            callbackMap.put("url", url);
        } catch (Exception e) {
            log.error("任务失败", e);
            updateTaskFailure(taskId, e.getMessage());
            callbackMap.put("success", false);
            callbackMap.put("error", e.getMessage());
        } finally {
            // 优雅关闭线程池和释放锁
            taskExecutor.shutdown();
            progressExecutor.shutdown();
            lockBucket.delete();

            // 确保 ExcelWriter 和输出流被正确关闭
            if (excelWriter != null) {
                try {
                    excelWriter.finish();
                } catch (Exception ex) {
                    log.error("关闭ExcelWriter异常", ex);
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (IOException e) {
                    log.error("关闭BufferedOutputStream异常", e);
                }
            }
        }

        log.info("任务结束: {}", title);
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
     * 通用的线程池创建方法
     */
    private ExecutorService createExecutorService(int corePoolSize, int maxPoolSize, int queueCapacity, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return TtlExecutors.getTtlExecutorService(executor.getThreadPoolExecutor());
    }

    /** 更新任务进度（只有当当前进度低于新进度时才更新） */
    private void updateTaskProgress(Integer taskId, int progress) {
        Taskcenter task = taskCenterManager.getTask(taskId);
        if (task.getProgress() < progress) {
            TaskCenterUpdateDto updateDto = new TaskCenterUpdateDto();
            updateDto.setId(taskId);
            updateDto.setStatus(1);
            updateDto.setProgress(progress);
            taskCenterManager.updateTask(updateDto);
        }
    }

    /** 更新任务完成状态 */
    private void updateTaskCompletion(Integer taskId, String url) {
        TaskCenterUpdateDto updateDto = new TaskCenterUpdateDto();
        updateDto.setId(taskId);
        updateDto.setStatus(4);
        updateDto.setProgress(100);
        updateDto.setUrl(url);
        updateDto.setErrorMsg("");
        taskCenterManager.updateTask(updateDto);
    }

    /** 更新任务失败状态 */
    private void updateTaskFailure(Integer taskId, String errorMsg) {
        TaskCenterUpdateDto updateDto = new TaskCenterUpdateDto();
        updateDto.setId(taskId);
        updateDto.setStatus(3);
        updateDto.setErrorMsg(errorMsg);
        taskCenterManager.updateTask(updateDto);
    }

    /** 创建临时 Excel 文件 */
    private File createExcelFile() throws IOException {
        String dirPath = System.getProperty("user.dir") + "/excel";
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String filePath = dirPath + "/" + UUID.randomUUID() + ExcelTypeEnum.XLSX.getValue();
        return new File(filePath);
    }

    /** 上传 Excel 文件并返回下载 URL */
    private String uploadExcelFile(File file, String title) throws IOException {
        String fileName = "excel/" + UUID.randomUUID().toString().replace("-", "") + "---" + title + ExcelTypeEnum.XLSX.getValue();
        IFileService fileService = SpringContextUtil.getBean(IFileService.class);
        try (InputStream is = Files.newInputStream(file.toPath())) {
            fileService.uploadCache(is, fileName);
        }
        fileService.submit(fileName);
        String url = fileService.getDownloadUrl(fileName);
        Files.deleteIfExists(file.toPath());
        return url;
    }

    /** 按指定大小分割列表 */
    private <T> List<List<T>> splitList(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        int total = list.size();
        for (int i = 0; i < total; i += batchSize) {
            batches.add(list.subList(i, Math.min(total, i + batchSize)));
        }
        return batches;
    }

}
