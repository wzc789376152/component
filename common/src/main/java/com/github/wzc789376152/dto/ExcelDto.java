package com.github.wzc789376152.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ExcelDto<T> {
    private  Map<Integer, String> head;
    private  List<T> dataList;
}
