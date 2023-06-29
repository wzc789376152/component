package com.github.wzc789376152.springboot.shardingjdbc;


import com.github.wzc789376152.springboot.config.SpringContextUtil;
import com.github.wzc789376152.springboot.config.shardingsphere.ShardingPropertics;
import com.github.wzc789376152.springboot.utils.ShardingUtils;
import com.github.wzc789376152.utils.DateUtils;
import com.google.common.collect.Range;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingValue;

import java.util.*;

@Slf4j
public class DateMonthRangeShardingAlgorithm implements RangeShardingAlgorithm<Date> {

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
}
