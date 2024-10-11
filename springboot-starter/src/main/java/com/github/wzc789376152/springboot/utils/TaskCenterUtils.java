package com.github.wzc789376152.springboot.utils;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.wzc789376152.exception.BizRuntimeException;
import com.github.wzc789376152.springboot.config.SpringContextUtil;
import com.github.wzc789376152.springboot.config.init.InitPropertice;
import com.github.wzc789376152.springboot.config.redis.IRedisService;
import com.github.wzc789376152.springboot.config.taskCenter.TaskCenterProperties;
import com.github.wzc789376152.springboot.taskCenter.ITaskCenterManager;
import com.github.wzc789376152.springboot.taskCenter.ITaskCenterService;
import com.github.wzc789376152.springboot.taskCenter.TaskCenterService;
import com.github.wzc789376152.springboot.taskCenter.dto.TaskCenterUpdateDto;
import com.github.wzc789376152.springboot.taskCenter.entity.Taskcenter;
import com.github.wzc789376152.springboot.taskCenter.mapper.TaskcenterMapper;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TaskCenterUtils {
    public static class Build {
        private Object service;

        private String serviceName;

        private String funcName;

        private String callbackFuncName;

        private String runUrl;

        public Build(Object service) {
            this.service = service;
        }

        public Build(String serviceName) {
            this.serviceName = serviceName;
        }

        public Build funcName(String funcName) {
            this.funcName = funcName;
            return this;
        }

        public <I, R> Build func(SFunction<I, R> sFunction) {
            this.funcName = LambdaUtils.extract(sFunction).getImplMethodName();
            return this;
        }

        public Build callbackFuncName(String funcName) {
            this.callbackFuncName = funcName;
            return this;
        }


        public <I, R> Build callbackFunc(SFunction<I, R> sFunction) {
            this.callbackFuncName = LambdaUtils.extract(sFunction).getImplMethodName();
            return this;
        }

        public Build runUrl(String runUrl) {
            this.runUrl = runUrl;
            return this;
        }

        public ITaskCenterService build() {
            TaskCenterProperties taskCenterProperties = SpringContextUtil.getBean(TaskCenterProperties.class);
            if (!taskCenterProperties.getEnable()) {
                throw new BizRuntimeException("未配置任务中心");
            }
            if (runUrl == null) {
                InitPropertice initPropertice = SpringContextUtil.getBean(InitPropertice.class);
                runUrl = "http://" + initPropertice.getServerName() + "/taskCenterBase/redo";
            }
            if (service != null) {
                return new TaskCenterService(service, funcName, callbackFuncName, runUrl);
            }
            if (serviceName != null) {
                try {
                    return new TaskCenterService(serviceName, funcName, callbackFuncName, runUrl);
                } catch (ClassNotFoundException exception) {
                    return null;
                }
            }
            return null;
        }
    }

    /**
     * 服务构造器
     *
     * @param service 服务对象
     * @param <T>     服务类型
     * @return T
     */
    public static <T> Build builder(T service) {
        return new Build(service);
    }

    public static <T> Build builder(String serviceName) {
        return new Build(serviceName);
    }

    /**
     * 查看列表
     *
     * @param wrapper  wrapper
     * @param pageNum  pageNum
     * @param pageSize pageSize
     * @return PageInfo
     */
    public static PageInfo<Taskcenter> findPage(Wrapper<Taskcenter> wrapper, Integer pageNum, Integer pageSize) {
        TaskCenterProperties taskCenterProperties = SpringContextUtil.getBean(TaskCenterProperties.class);
        if (!taskCenterProperties.getEnable()) {
            throw new BizRuntimeException("未配置任务中心");
        }
        TaskcenterMapper taskcenterMapper = SpringContextUtil.getBean(TaskcenterMapper.class);
        return PageUtils.page(pageNum, pageSize).start(() -> taskcenterMapper.selectList(wrapper)).result(Taskcenter.class);
    }

    /**
     * 重启任务
     *
     * @param id id
     */
    public static void redo(Integer id, Integer timer) {
        if (timer == null) {
            timer = 0;
        }
        TaskCenterProperties taskCenterProperties = SpringContextUtil.getBean(TaskCenterProperties.class);
        if (!taskCenterProperties.getEnable()) {
            throw new BizRuntimeException("未配置任务中心");
        }
        ITaskCenterManager taskcenterMapper = SpringContextUtil.getBean(ITaskCenterManager.class);
        Taskcenter taskcenter = taskcenterMapper.getTask(id);
        if (taskcenter == null) {
            throw new BizRuntimeException("任务不存在");
        }
        try {
            SpringContextUtil.getBean(Class.forName(taskcenter.getServiceName()));
        } catch (Exception e) {
            RestTemplate restTemplate = SpringContextUtil.getBean(RestTemplate.class);
            Boolean isRedo = restTemplate.getForObject(taskcenter.getRunUrl() + "?id=" + id + "&timer=" + (timer + 1), Boolean.class);
            if (Boolean.FALSE.equals(isRedo)) {
                throw new RuntimeException("服务不存在");
            }
            return;
        }
        TaskCenterUpdateDto taskCenterUpdateDto = new TaskCenterUpdateDto();
        taskCenterUpdateDto.setId(id);
        taskCenterUpdateDto.setStatus(2);
        taskcenterMapper.updateTask(taskCenterUpdateDto);
        //通过缓存拿请求参数
        JSONObject paramObj = getParam(id);
        String name = paramObj.getString("name");
        String data = paramObj.getString("data");
        List<?> params;
        try {
            Class<?> clazz = Class.forName(name);
            params = JSONArray.parseArray(data, clazz);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        builder(taskcenter.getServiceName()).funcName(taskcenter.getServiceMethod()).callbackFuncName(taskcenter.getCallbackServiceMethod()).build().runAsync(taskcenter.getTitle(), taskcenter.getId(), params);
    }

    public static JSONObject getParam(Integer id) {
        //通过缓存拿请求参数
        IRedisService redisService = SpringContextUtil.getBean(IRedisService.class);
        InitPropertice initPropertice = SpringContextUtil.getBean(InitPropertice.class);
        String key = "taskCenter:" + initPropertice.getServerName() + ":serviceParam:" + id;
        JSONObject paramObj = CurrentHashMapUtil.get(key, JSONObject.class);
        if (paramObj == null) {
            paramObj = redisService.getCacheObject(key, JSONObject.class);
            if (paramObj == null) {
                TaskCenterUpdateDto taskCenterUpdateDto = new TaskCenterUpdateDto();
                taskCenterUpdateDto.setId(id);
                taskCenterUpdateDto.setStatus(3);
                taskCenterUpdateDto.setErrorMsg("请求参数不存在");
                ITaskCenterManager taskcenterMapper = SpringContextUtil.getBean(ITaskCenterManager.class);
                taskcenterMapper.updateTask(taskCenterUpdateDto);
                throw new BizRuntimeException("请求参数不存在");
            }
            CurrentHashMapUtil.put(key, paramObj, 1L, TimeUnit.DAYS);
        }
        return paramObj;
    }

    /**
     * 删除任务
     *
     * @param id id
     */
    public static void remove(Integer id) {
        TaskCenterProperties taskCenterProperties = SpringContextUtil.getBean(TaskCenterProperties.class);
        if (!taskCenterProperties.getEnable()) {
            throw new BizRuntimeException("未配置任务中心");
        }
        TaskcenterMapper taskcenterMapper = SpringContextUtil.getBean(TaskcenterMapper.class);
        taskcenterMapper.deleteById(id);
    }
}
