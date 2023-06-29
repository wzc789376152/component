package com.github.wzc789376152.springboot.utils;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.github.wzc789376152.springboot.shardingjdbc.IShardingService;
import com.github.wzc789376152.springboot.shardingjdbc.ShardingService;
import com.github.wzc789376152.springboot.shardingjdbc.ShardingType;
import com.github.wzc789376152.springboot.shardingjdbc.function.ShardingCountFunction;
import com.github.wzc789376152.springboot.shardingjdbc.function.ShardingListFunction;
import com.github.wzc789376152.utils.DateUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

public class ShardingUtils {


    public static TreeSet<String> getSuffixListForRange(String lowerSuffix, String upperSuffix) {
        TreeSet<String> suffixList = new TreeSet<>();
        if (lowerSuffix.equals(upperSuffix)) { //上下界在同一张表
            suffixList.add(lowerSuffix);
        } else {  //上下界不在同一张表  计算间隔的所有表
            String tempSuffix = lowerSuffix;
            while (!tempSuffix.equals(upperSuffix)) {
                suffixList.add(tempSuffix);
                String[] ym = tempSuffix.split("_");
                Date tempDate = DateUtils.parse(ym[0] + (ym[1].length() == 1 ? "0" + ym[1] : ym[1]), "yyyyMM");
                Calendar cal = Calendar.getInstance();
                cal.setTime(tempDate);
                cal.add(Calendar.MONTH, 1);
                tempSuffix = ShardingUtils.getSuffixByYearMonth(cal.getTime());
            }
            suffixList.add(tempSuffix);
        }
        return suffixList;
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
