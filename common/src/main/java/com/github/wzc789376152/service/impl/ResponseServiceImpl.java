package com.github.wzc789376152.service.impl;

import com.github.wzc789376152.service.IResponseService;
import com.github.wzc789376152.vo.RetResult;

public class ResponseServiceImpl implements IResponseService {

    @Override
    public Class<?> getResultClass() {
        return RetResult.class;
    }

    @Override
    public <D> Object success(D data, String msg) {
        return success(null, data, msg);
    }

    @Override
    public <D> RetResult<D> success(String uuid, D data, String msg) {
        return RetResult.success(uuid, data, msg);
    }

    @Override
    public Object error(Integer code, String msg) {
        return error(null, code, msg);
    }

    @Override
    public RetResult<Object> error(String uuid, Integer code, String msg) {
        return RetResult.failed(uuid, code, msg, null);
    }
}
