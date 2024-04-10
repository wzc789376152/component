package com.github.wzc789376152.springboot.taskCenter.dto;

import lombok.Data;

@Data
public class TaskCenterInitDto {
    Integer id;
    String title;
    String serviceName;
    String funcName;
    String callbackFuncName;
//    String serviceParam;
    String runUrl;
}
