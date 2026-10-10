package com.github.wzc789376152.springboot.taskCenter.dto;

import lombok.Data;

@Data
public class TaskCenterCallBackDto {
    private Integer taskId;
    private String data;
    private Long startTime;
    private Long endTime;
    private Boolean success;
    private String url;
    private String error;
}
