package com.github.wzc789376152.springboot.shardingjdbc;


import com.github.wzc789376152.springboot.config.SpringContextUtil;
import com.github.wzc789376152.springboot.config.shardingsphere.ShardingPropertics;
import com.github.wzc789376152.springboot.utils.ShardingUtils;
import com.github.wzc789376152.utils.DateUtils;
import com.google.common.collect.Range;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component(value = "dateMonthShardingAlgorithm")
public class DateMonthShardingAlgorithm implements StandardShardingAlgorithm<Date> {

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Date> rangeShardingValue) {
        Set<String> hashSet = new HashSet<>();
        Range<Date> valueRange = rangeShardingValue.getValueRange();
        //超界取当前时间
        Date upperDate = valueRange.hasUpperBound() ? valueRange.upperEndpoint() : new Date();
        if (upperDate.getTime() > System.currentTimeMillis()) {
            upperDate = new Date();
        }
        ShardingPropertics shardingPropertics = SpringContextUtil.getBean(ShardingPropertics.class);
        //超界取2020-01-01
        Date lowerDate = valueRange.hasLowerBound() ? valueRange.lowerEndpoint() : (shardingPropertics.getMinDate() == null ? DateUtils.parse("2000-01-01", "yyyy-MM-dd") : shardingPropertics.getMinDate());
        String lowerSuffix = ShardingUtils.getSuffixByYearMonth(lowerDate);
        String upperSuffix = ShardingUtils.getSuffixByYearMonth(upperDate);
        TreeSet<String> suffixList = ShardingUtils.getSuffixListForRange(lowerSuffix, upperSuffix);
        for (String tableName : availableTargetNames) {
            if (containTableName(suffixList, tableName)) {
                hashSet.add(tableName);
            }
        }
        return hashSet;
    }

    private boolean containTableName(Set<String> suffixList, String tableName) {
        for (String s : suffixList) {
            if (tableName.endsWith(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<Date> preciseShardingValue) {
        Date date = preciseShardingValue.getValue();
        Date upperDate = new Date();
        Date lowerDate = DateUtils.parse("2000-01-01", "yyyy-MM-dd");
        ShardingPropertics shardingPropertics = SpringContextUtil.getBean(ShardingPropertics.class);
        if (shardingPropertics.getMinDate() != null) {
            lowerDate = shardingPropertics.getMinDate();
        }
        if (date.getTime() > upperDate.getTime()) {
            date = upperDate;
        }
        if (date.getTime() < lowerDate.getTime()) {
            date = lowerDate;
        }
        String suffix = ShardingUtils.getSuffixByYearMonth(date);
        for (String tableName : collection) {
            if (tableName.endsWith(suffix)) {
                return tableName;
            }
        }
        throw new IllegalArgumentException("未找到匹配的数据表");
    }

    @Override
    public void init() {

    }

    @Override
    public String getType() {
        return null;
    }
}
