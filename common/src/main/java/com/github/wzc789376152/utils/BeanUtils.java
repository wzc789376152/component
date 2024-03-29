package com.github.wzc789376152.utils;


import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.util.Assert;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class BeanUtils extends org.springframework.beans.BeanUtils {
    public static <T> T copyBean(Object source, T target) {
        copyProperties(source, target);
        return target;
    }

    /**
     * 功能描述: 忽略NULL值与空值<br>
     * 〈功能详细描述〉
     *
     * @param source source
     * @param target target
     * @throws BeansException 异常
     */
    public static void copyProperties(Object source, Object target) throws BeansException {
        Assert.notNull(source, "Source must not be null");
        Assert.notNull(target, "Target must not be null");
        Class<?> actualEditable = target.getClass();
        PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
        for (PropertyDescriptor targetPd : targetPds) {
            if (targetPd.getWriteMethod() != null) {
                PropertyDescriptor sourcePd = getPropertyDescriptor(source.getClass(), targetPd.getName());
                if (sourcePd != null && sourcePd.getReadMethod() != null) {
                    try {
                        Method readMethod = sourcePd.getReadMethod();
                        if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
                            readMethod.setAccessible(true);
                        }
                        Object value = readMethod.invoke(source);
                        // 这里判断以下value是否为空 当然这里也能进行一些特殊要求的处理 例如绑定时格式转换等等
                        if (value != null) {
                           /* if (value instanceof String && ((String) value).isEmpty()) {
                                continue;
                            }*/
                            Method writeMethod = targetPd.getWriteMethod();
                            if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
                                writeMethod.setAccessible(true);
                            }
                            writeMethod.invoke(target, value);
                        }
                    } catch (SecurityException e) {
                        throw new FatalBeanException("Could not copy properties from source to target", e);
                    } catch (IllegalAccessException e) {
                        throw new FatalBeanException("Could not copy properties from source to target", e);
                    } catch (IllegalArgumentException e) {
                        throw new FatalBeanException("Could not copy properties from source to target", e);
                    } catch (InvocationTargetException e) {
                        throw new FatalBeanException("Could not copy properties from source to target", e);
                    }
                }
            }
        }
    }
}
