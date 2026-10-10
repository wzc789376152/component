package com.github.wzc789376152.springboot.taskCenter.function;

import java.io.Serializable;
import java.util.List;

@FunctionalInterface
public interface TaskCenterFunction<T, P1, R extends List<T>> extends Serializable {
    R apply(P1 p1, T p2);
}
