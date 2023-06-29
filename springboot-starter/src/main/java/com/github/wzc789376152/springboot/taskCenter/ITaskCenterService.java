package com.github.wzc789376152.springboot.taskCenter;

import java.util.List;

public interface ITaskCenterService<T> {
    <P> void runAsync(String title, P param);

    <P> void runAsync(String title, List<P> params);

    <P> void runAsync(String title, Integer taskId, P param);

    <P> void runAsync(String title, Integer taskId, List<P> params);
}
