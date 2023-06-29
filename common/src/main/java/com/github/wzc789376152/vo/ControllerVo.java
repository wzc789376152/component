package com.github.wzc789376152.vo;

import lombok.Data;

@Data
public class ControllerVo extends TreeVo<ControllerVo> {
    private String url;
    private String active;
}

