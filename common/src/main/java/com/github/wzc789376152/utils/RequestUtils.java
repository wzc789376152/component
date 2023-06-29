package com.github.wzc789376152.utils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * <p>
 * Http请求工具类
 * </p>
 *
 * @author jiangjianhe
 * @since 2022-07-25
 */
public final class RequestUtils {

    /**
     * 获取所有request请求参数key-value
     *
     * @param request httpRequest
     * @return map
     */
    public static Map<String, String> getRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        if (null != request) {
            Set<String> paramsKey = request.getParameterMap().keySet();
            for (String key : paramsKey) {
                params.put(key, request.getParameter(key));
            }
        }
        return params;
    }
}
