package com.github.wzc789376152.springboot.utils;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.github.wzc789376152.springboot.shardingjdbc.IShardingService;
import com.github.wzc789376152.springboot.shardingjdbc.ShardingService;
import com.github.wzc789376152.springboot.shardingjdbc.ShardingType;
import com.github.wzc789376152.springboot.shardingjdbc.function.ShardingCountFunction;
import com.github.wzc789376152.springboot.shardingjdbc.function.ShardingListFunction;
import com.github.wzc789376152.utils.DateUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

public class ShardingUtils {


    /**
     * 根据下界和上界后缀，生成中间所有的后缀集合（包含边界值）。
     * 假设后缀格式为 "yyyy_M" 或 "yyyy_MM"，例如 "2022_1" 或 "2022_12"。
     *
     * @param lowerSuffix 下界后缀
     * @param upperSuffix 上界后缀
     * @return 包含所有后缀的 TreeSet 集合
     */
    public static TreeSet<String> getSuffixListForRange(String lowerSuffix, String upperSuffix) {
        TreeSet<String> suffixList = new TreeSet<>();

        // 解析后缀为 YearMonth
        YearMonth lower = parseSuffix(lowerSuffix);
        YearMonth upper = parseSuffix(upperSuffix);

        // 遍历从 lower 到 upper 的所有月份
        for (YearMonth ym = lower; !ym.isAfter(upper); ym = ym.plusMonths(1)) {
            // 将 YearMonth 转换为 Date（假设需要传给 ShardingUtils.getSuffixByYearMonth）
            Date date = Date.from(ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            String suffix = ShardingUtils.getSuffixByYearMonth(date);
            suffixList.add(suffix);
        }
        return suffixList;
    }

    /**
     * 将形如 "yyyy_M" 或 "yyyy_MM" 的后缀转换为 YearMonth 对象
     *
     * @param suffix 后缀字符串
     * @return 对应的 YearMonth 对象
     */
    private static YearMonth parseSuffix(String suffix) {
        String[] parts = suffix.split("_");
        if (parts.length != 2) {
            throw new IllegalArgumentException("后缀格式错误: " + suffix);
        }
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        return YearMonth.of(year, month);
    }

    public static int getSuffixByYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    public static String getSuffixByYearMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR) + "_" + (calendar.get(Calendar.MONTH) + 1);
    }

    public static String getPrevSuffix(String suffix) {
        if (StringUtils.isBlank(suffix)) {
            return getSuffixByYearMonth(new Date());
        }
        String[] arr = suffix.split("_");
        if ("1".equals(arr[1])) {
            return (Integer.parseInt(arr[0]) - 1) + "_12";
        } else {
            return arr[0] + "_" + (Integer.parseInt(arr[1]) - 1);
        }
    }

    /**
     * shardingService构造器
     *
     * @param <T> T
     */
    public static class Builder<T> {
        private ShardingCountFunction< Wrapper, Integer> countMethod;
        private ShardingListFunction<Wrapper, Integer, Integer, List<T>> listMethod;
        private String field;
        private Date startTime;
        private Date endTime;
        private String asyncName;
        private String searchAsyncName;
        private String countAsyncName;

        private ShardingType shardingType;


        public Builder<T> count(ShardingCountFunction<Wrapper, Integer> countMethod) {
            this.countMethod = countMethod;
            return this;
        }

        public Builder<T> list(ShardingListFunction<Wrapper, Integer, Integer, List<T>> listMethod) {
            this.listMethod = listMethod;
            return this;
        }

        public Builder<T> field(String field) {
            this.field = field;
            return this;
        }

        public Builder<T> start(Date startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder<T> end(Date endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder<T> async(String asyncName) {
            this.asyncName = asyncName;
            return this;
        }

        public Builder<T> searchAsync(String searchAsyncName) {
            this.searchAsyncName = searchAsyncName;
            return this;
        }

        public Builder<T> countAsync(String countAsyncName) {
            this.countAsyncName = countAsyncName;
            return this;
        }
        public Builder<T> shardingType(ShardingType shardingType){
            this.shardingType = shardingType;
            return this;
        }

        public IShardingService<T> build() {
            if (this.searchAsyncName == null) {
                this.searchAsyncName = "shardingSearchAsync";
            }
            if (this.countAsyncName == null) {
                this.countAsyncName = "shardingCountAsync";
            }
            if (this.asyncName == null) {
                this.asyncName = "shardingAsync";
            }
            if(this.shardingType == null){
                this.shardingType = ShardingType.Year;
            }
            return new ShardingService<>(this.field, this.startTime, this.endTime, this.countMethod, this.listMethod, this.asyncName, this.searchAsyncName, this.countAsyncName,this.shardingType);
        }
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
}
