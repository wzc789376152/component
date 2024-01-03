package com.github.wzc789376152.springboot.taskCenter;

import com.github.wzc789376152.springboot.taskCenter.dto.TaskCenterInitDto;
import com.github.wzc789376152.springboot.taskCenter.dto.TaskCenterUpdateDto;
import com.github.wzc789376152.springboot.taskCenter.entity.Taskcenter;

public interface ITaskCenterManager {
    Integer initTask(TaskCenterInitDto taskCenterInitDto);

    Taskcenter getTask(Integer id);

    void updateTask(TaskCenterUpdateDto taskCenterUpdateDto);

}
