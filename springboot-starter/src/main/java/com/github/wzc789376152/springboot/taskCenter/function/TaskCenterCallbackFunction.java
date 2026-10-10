package com.github.wzc789376152.springboot.taskCenter.function;

import com.github.wzc789376152.springboot.taskCenter.dto.TaskCenterCallBackDto;

import java.io.Serializable;

@FunctionalInterface
public interface TaskCenterCallbackFunction<R extends TaskCenterCallBackDto> extends Serializable {
    void apply(R r);
}
