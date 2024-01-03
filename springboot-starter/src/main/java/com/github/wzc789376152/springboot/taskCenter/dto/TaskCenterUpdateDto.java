package com.github.wzc789376152.springboot.taskCenter.dto;

import lombok.Data;

@Data
public class TaskCenterUpdateDto {
    Integer id;
    Integer progress;
    Integer status;
    String errorMsg;
    String url;
}
