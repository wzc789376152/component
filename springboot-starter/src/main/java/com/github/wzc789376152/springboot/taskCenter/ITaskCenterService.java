package com.github.wzc789376152.springboot.taskCenter;

import java.util.List;

public interface ITaskCenterService {

    <P> Integer initTask(Integer id, String title, P param);

    <P> Integer initTask(String title, List<P> param);

    <P> Integer initTask(String title, P param);

    <P> Integer initTask(Integer id, String title, List<P> params);

    void runAsync(Integer taskId);

    <P> void runAsync(String title, P param);

    <P> void runAsync(String title, List<P> params);

    <P> void runAsync(String title, Integer taskId, P param);

    <P> void runAsync(String title, Integer taskId, List<P> params);
}
