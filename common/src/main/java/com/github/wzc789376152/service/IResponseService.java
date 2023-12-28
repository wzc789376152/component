package com.github.wzc789376152.service;

import org.apache.poi.ss.formula.functions.T;

public interface IResponseService {
    Class<?> getResultClass();

    <D> Object success(D data, String msg);

    <D> Object success(String uuid, D data, String msg);

    Object error(Integer code, String msg);

    Object error(String uuid, Integer code, String msg);
}
