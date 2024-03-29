package com.github.wzc789376152.springboot.utils;


import com.github.wzc789376152.exception.BizRuntimeException;
import com.github.wzc789376152.springboot.config.SpringContextUtil;
import com.github.wzc789376152.springboot.config.init.InitPropertice;
import com.github.wzc789376152.springboot.config.redis.IRedisService;
import com.github.wzc789376152.springboot.config.sequence.SequenceProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SequenceUtils {
    private static final SnowflakeIdWorker snowflakeIdWorker = new SnowflakeIdWorker(new Random().nextInt(30), new Random().nextInt(30));

    public static String getCode(String key) {
        SequenceProperty sequenceProperty = SpringContextUtil.getBean(SequenceProperty.class);
        if (!sequenceProperty.getEnable()) {
            throw new BizRuntimeException("未启用生成配置");
        }
        if (sequenceProperty.getType() == null) {
            throw new BizRuntimeException("缺少生成配置类型");
        }
        InitPropertice initPropertice = SpringContextUtil.getBean(InitPropertice.class);
        if (!initPropertice.getProfilesActive().equals("prod")) {
            key = key + "DEV";
        }
        switch (sequenceProperty.getType()) {
            case REDIS:
                return createByRedis(key, sequenceProperty.getLength());
            case ASSIGN_ID:
                return createByAssign(key);
            default:
                throw new BizRuntimeException("类型错误");
        }
    }

    private static String createByRedis(String key, Integer length) {
        LocalDate localDate = LocalDate.now();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMdd");
        StringBuilder keyValue = new StringBuilder(key + df.format(localDate));
        try {
            IRedisService redisService = SpringContextUtil.getBean(IRedisService.class);
            InitPropertice initPropertice = SpringContextUtil.getBean(InitPropertice.class);
            String redisKey = "wzc:" + "sequence:" + initPropertice.getServerName() + ":" + keyValue;
            Long sequence = redisService.increment(redisKey, key, 1L);
            redisService.expire(redisKey, 2, TimeUnit.DAYS);
            for (int i = 0; i < length - sequence.toString().length(); i++) {
                keyValue.append(0);
            }
            keyValue.append(sequence);
        } catch (BeansException e) {
            log.error("未配置redis", e);
            throw new BizRuntimeException("未配置redis");
        }
        return keyValue.toString();
    }

    private static String createByAssign(String key) {
        return key + snowflakeIdWorker.nextId();
    }

    private static class SnowflakeIdWorker {

        // ==============================Fields===========================================

        /**
         * 机器id所占的位数
         */
        private final long workerIdBits = 5L;

        /**
         * 数据标识id所占的位数
         */
        private final long datacenterIdBits = 5L;

        /**
         * 工作机器ID(0~31)
         */
        private final long workerId;

        /**
         * 数据中心ID(0~31)
         */
        private final long datacenterId;

        /**
         * 毫秒内序列(0~4095)
         */
        private long sequence = 0L;

        /**
         * 上次生成ID的时间截
         */
        private long lastTimestamp = -1L;

        // ==============================Constructors=====================================

        /**
         * 构造函数
         *
         * @param workerId     工作ID (0~31)
         * @param datacenterId 数据中心ID (0~31)
         */
        public SnowflakeIdWorker(long workerId, long datacenterId) {
            /**
             * 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
             */
            long maxWorkerId = ~(-1L << workerIdBits);
            if (workerId > maxWorkerId || workerId < 0) {
                throw new IllegalArgumentException(
                        String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
            }
            /**
             * 支持的最大数据标识id，结果是31
             */
            long maxDatacenterId = ~(-1L << datacenterIdBits);
            if (datacenterId > maxDatacenterId || datacenterId < 0) {
                throw new IllegalArgumentException(
                        String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
            }
            this.workerId = workerId;
            this.datacenterId = datacenterId;
        }

        // ==============================Methods==========================================

        /**
         * 获得下一个ID (该方法是线程安全的)
         *
         * @return SnowflakeId
         */
        public synchronized Long nextId() {
            long timestamp = timeGen();

            // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
            if (timestamp < lastTimestamp) {
                throw new RuntimeException(String.format(
                        "Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
            }

            // 如果是同一时间生成的，则进行毫秒内序列
            /**
             * 序列在id中占的位数
             */
            long sequenceBits = 12L;
            if (lastTimestamp == timestamp) {
                /**
                 * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
                 */
                long sequenceMask = ~(-1L << sequenceBits);
                sequence = (sequence + 1) & sequenceMask;
                // 毫秒内序列溢出
                if (sequence == 0) {
                    // 阻塞到下一个毫秒,获得新的时间戳
                    timestamp = tilNextMillis(lastTimestamp);
                }
            }
            // 时间戳改变，毫秒内序列重置
            else {
                sequence = 0L;
            }

            // 上次生成ID的时间截
            lastTimestamp = timestamp;

            // 移位并通过或运算拼到一起组成64位的ID
            /**
             * 开始时间截 (2015-01-01)
             */
            long twepoch = 1420041600000L;
            /**
             * 机器ID向左移12位
             */
            /**
             * 数据标识id向左移17位(12+5)
             */
            long datacenterIdShift = sequenceBits + workerIdBits;
            /**
             * 时间截向左移22位(5+5+12)
             */
            long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
            return ((timestamp - twepoch) << timestampLeftShift) //
                    | (datacenterId << datacenterIdShift) //
                    | (workerId << sequenceBits) //
                    | sequence;
        }

        /**
         * 阻塞到下一个毫秒，直到获得新的时间戳
         *
         * @param lastTimestamp 上次生成ID的时间截
         * @return 当前时间戳
         */
        protected long tilNextMillis(long lastTimestamp) {
            long timestamp = timeGen();
            while (timestamp <= lastTimestamp) {
                timestamp = timeGen();
            }
            return timestamp;
        }

        /**
         * 返回以毫秒为单位的当前时间
         *
         * @return 当前时间(毫秒)
         */
        protected long timeGen() {
            return System.currentTimeMillis();
        }
    }
}
