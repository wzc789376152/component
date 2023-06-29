package com.github.wzc789376152.springboot.shardingjdbc.function;

import com.baomidou.mybatisplus.core.conditions.Wrapper;

import java.util.List;

@FunctionalInterface
public interface ShardingListFunction<P1 extends Wrapper, P2 extends Integer, P3 extends Integer, R extends List> {
    R apply(P1 p1, P2 p2, P3 p3);
}
