package com.github.wzc789376152.service.impl;

import com.github.wzc789376152.service.IResponseService;
import com.github.wzc789376152.vo.RetResult;

public class ResponseServiceImpl implements IResponseService {

    @Override
    public Class<?> getResultClass() {
        return RetResult.class;
    }

    @Override
    public <D> RetResult<D> success(D data,String msg) {
        return RetResult.success(data,msg);
    }

    @Override
    public RetResult<Object> error(Integer code, String msg) {
        return RetResult.failed(code, msg, null);
    }
}
