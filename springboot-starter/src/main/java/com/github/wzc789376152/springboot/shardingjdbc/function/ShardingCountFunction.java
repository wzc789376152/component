package com.github.wzc789376152.springboot.shardingjdbc.function;

import com.baomidou.mybatisplus.core.conditions.Wrapper;

@FunctionalInterface
public interface ShardingCountFunction<P1 extends Wrapper, R extends Integer> {
    R apply(P1 p1);
}

