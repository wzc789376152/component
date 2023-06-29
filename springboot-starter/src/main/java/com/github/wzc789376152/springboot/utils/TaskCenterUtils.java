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
import com.github.wzc789376152.springboot.config.taskCenter.TaskCenterProperties;
import com.github.wzc789376152.springboot.taskCenter.ITaskCenterService;
import com.github.wzc789376152.springboot.taskCenter.TaskCenterService;
import com.github.wzc789376152.springboot.taskCenter.entity.Taskcenter;
import com.github.wzc789376152.springboot.taskCenter.mapper.TaskcenterMapper;

import java.util.List;

public class TaskCenterUtils {
    public static class Build<T> {
        private T service;

        private String serviceName;

        private String funcName;

        private String callbackFuncName;

        public Build(T service) {
            this.service = service;
        }

        public Build(String serviceName) {
            this.serviceName = serviceName;
        }

        public Build<T> funcName(String funcName) {
            this.funcName = funcName;
            return this;
        }

        public <I, R> Build<T> func(SFunction<I, R> sFunction) {
            this.funcName = LambdaUtils.extract(sFunction).getImplMethodName();
            return this;
        }

        public Build<T> callbackFuncName(String funcName) {
            this.callbackFuncName = funcName;
            return this;
        }


        public <I, R> Build<T> callbackFunc(SFunction<I, R> sFunction) {
            this.callbackFuncName = LambdaUtils.extract(sFunction).getImplMethodName();
            return this;
        }

        public ITaskCenterService<T> build() {
            TaskCenterProperties taskCenterProperties = SpringContextUtil.getBean(TaskCenterProperties.class);
            if (!taskCenterProperties.getEnable()) {
                throw new BizRuntimeException("未配置任务中心");
            }
            if (service != null) {
                return new TaskCenterService<>(service, funcName, callbackFuncName);
            }
            if (serviceName != null) {
                return new TaskCenterService<>(serviceName, funcName, callbackFuncName);
            }
            return null;
        }
    }

    /**
     * 服务构造器
     * @param service 服务对象
     * @return T
     * @param <T> 服务类型
     */
    public static <T> Build<T> builder(T service) {
        return new Build<>(service);
    }

    public static <T> Build<T> builder(String serviceName) {
        return new Build<>(serviceName);
    }

    /**
     * 查看列表
     * @param wrapper wrapper
     * @param pageNum pageNum
     * @param pageSize pageSize
     * @return PageInfo
     */
    public static PageInfo<Taskcenter> findPage(Wrapper<Taskcenter> wrapper, Integer pageNum, Integer pageSize) {
        TaskCenterProperties taskCenterProperties = SpringContextUtil.getBean(TaskCenterProperties.class);
        if (!taskCenterProperties.getEnable()) {
            throw new BizRuntimeException("未配置任务中心");
        }
        TaskcenterMapper taskcenterMapper = SpringContextUtil.getBean(TaskcenterMapper.class);
        Page<Taskcenter> page = PageHelper.startPage(pageNum, pageSize);
        taskcenterMapper.selectList(wrapper);
        return PageInfo.of(page);
    }

    /**
     * 重启任务
     * @param id id
     */
    public static void redo(Integer id) {
        TaskCenterProperties taskCenterProperties = SpringContextUtil.getBean(TaskCenterProperties.class);
        if (!taskCenterProperties.getEnable()) {
            throw new BizRuntimeException("未配置任务中心");
        }
        TaskcenterMapper taskcenterMapper = SpringContextUtil.getBean(TaskcenterMapper.class);
        Taskcenter taskcenter = taskcenterMapper.selectById(id);
        if (taskcenter == null) {
            throw new BizRuntimeException("任务不存在");
        }
        try {
            SpringContextUtil.getBean(taskcenter.getServiceName().substring(0, 1).toLowerCase() + taskcenter.getServiceName().substring(1));
        } catch (Exception e) {
            Taskcenter taskcenter1 = new Taskcenter();
            taskcenter1.setId(taskcenter.getId());
            taskcenter1.setStatus(3);
            taskcenter1.setErrorMsg("服务不存在");
            throw new BizRuntimeException("服务不存在");
        }
        Taskcenter taskcenter1 = new Taskcenter();
        taskcenter1.setId(id);
        taskcenter1.setStatus(2);
        taskcenterMapper.updateById(taskcenter1);
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

        builder(taskcenter.getServiceName()).funcName(taskcenter.getServiceMethod()).callbackFuncName(taskcenter.getCallbackServiceMethod()).build().runAsync(taskcenter.getTitle(), taskcenter.getId(), params);
    }

    /**
     * 删除任务
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
