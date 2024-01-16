package com.github.wzc789376152.springboot.taskCenter;

import com.alibaba.fastjson.JSONObject;
import com.github.wzc789376152.springboot.taskCenter.dto.TaskCenterInitDto;
import com.github.wzc789376152.springboot.taskCenter.dto.TaskCenterUpdateDto;
import com.github.wzc789376152.springboot.taskCenter.entity.Taskcenter;
import com.github.wzc789376152.springboot.taskCenter.mapper.TaskcenterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

public class TaskCenterManagerImpl implements ITaskCenterManager {
    private TaskcenterMapper taskcenterMapper;

    public TaskCenterManagerImpl(TaskcenterMapper taskcenterMapper) {
        this.taskcenterMapper = taskcenterMapper;
    }

    @Override
    public Integer initTask(TaskCenterInitDto taskCenterInitDto) {
        Taskcenter taskcenter = null;
        if (taskCenterInitDto.getId() != null) {
            taskcenter = taskcenterMapper.selectById(taskCenterInitDto.getId());
        }
        if (taskcenter == null) {
            taskcenter = new Taskcenter();
            taskcenter.setTitle(taskCenterInitDto.getTitle());
            taskcenter.setServiceName(taskCenterInitDto.getServiceName());
            taskcenter.setServiceMethod(taskCenterInitDto.getFuncName());
            taskcenter.setCallbackServiceMethod(taskCenterInitDto.getCallbackFuncName());
            taskcenter.setRunUrl(taskCenterInitDto.getRunUrl());
            taskcenter.setServiceParam(taskCenterInitDto.getServiceParam());
            taskcenter.setProgress(0);
            taskcenter.setStatus(0);
            taskcenterMapper.insert(taskcenter);
        }
        return taskcenter.getId();
    }

    @Override
    public Taskcenter getTask(Integer id) {
        return taskcenterMapper.selectById(id);
    }

    @Override
    public void updateTask(TaskCenterUpdateDto taskCenterUpdateDto) {
        Taskcenter taskcenter1 = new Taskcenter();
        taskcenter1.setId(taskCenterUpdateDto.getId());
        taskcenter1.setStatus(taskCenterUpdateDto.getStatus());
        taskcenter1.setProgress(taskCenterUpdateDto.getProgress());
        taskcenter1.setFinishTime(taskCenterUpdateDto.getStatus() == 4 ? new Date() : null);
        taskcenter1.setUrl(taskCenterUpdateDto.getUrl());
        taskcenter1.setErrorMsg(taskCenterUpdateDto.getErrorMsg());
        taskcenterMapper.updateById(taskcenter1);
    }
}
