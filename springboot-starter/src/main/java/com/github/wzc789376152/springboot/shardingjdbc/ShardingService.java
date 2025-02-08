package com.github.wzc789376152.springboot.shardingjdbc;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageInfo;
import com.github.wzc789376152.springboot.config.SpringContextUtil;
import com.github.wzc789376152.springboot.config.shardingsphere.ShardingPropertics;
import com.github.wzc789376152.springboot.shardingjdbc.function.ShardingCountFunction;
import com.github.wzc789376152.springboot.shardingjdbc.function.ShardingListFunction;
import com.github.wzc789376152.utils.DateUtils;
import lombok.Data;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * 分页搜索服务类（不支持 group by 查询）
 */
public class ShardingService<T> implements IShardingService<T> {

    private final ExecutorService executor;
    private final ExecutorService searchExecutor;
    private final ExecutorService countExecutor;
    private final ShardingCountFunction<Wrapper, Integer> countMethod;
    private final ShardingListFunction<Wrapper, Integer, Integer, List<T>> listMethod;
    private final String field;
    private final ShardingType shardingType;

    // 构造时传入的初始时间范围（可为 null）
    private final Date initStartTime;
    private final Date initEndTime;

    public ShardingService(String field,
                           Date startTime,
                           Date endTime,
                           ShardingCountFunction<Wrapper, Integer> countMethod,
                           ShardingListFunction<Wrapper, Integer, Integer, List<T>> listMethod,
                           String asyncName,
                           String searchAsyncName,
                           String countAsyncName,
                           ShardingType shardingType) {
        this.field = field;
        this.initStartTime = startTime;
        this.initEndTime = endTime;
        this.countMethod = countMethod;
        this.listMethod = listMethod;
        this.shardingType = shardingType;
        this.executor = SpringContextUtil.getBean(asyncName, ExecutorService.class);
        this.countExecutor = SpringContextUtil.getBean(countAsyncName, ExecutorService.class);
        this.searchExecutor = SpringContextUtil.getBean(searchAsyncName, ExecutorService.class);
    }

    @Override
    public Integer queryCount(QueryWrapper<?> wrapper) throws ExecutionException, InterruptedException {
        return queryCountAsync(wrapper).get();
    }

    @Override
    public List<T> queryList(QueryWrapper<?> wrapper, Integer pageNum, Integer pageSize) throws ExecutionException, InterruptedException {
        return queryListAsync(wrapper, pageNum, pageSize).get();
    }

    /**
     * 异步统计各分片数据总数，并汇总返回。
     */
    @Override
    public Future<Integer> queryCountAsync(QueryWrapper<?> wrapper) {
        // 克隆传入的 QueryWrapper，避免对外部状态产生影响
        QueryWrapper<?> baseWrapper = wrapper.clone();
        // 根据查询条件调整有效的时间边界
        Date effectiveStart = computeEffectiveStartTime(baseWrapper, initStartTime);
        Date effectiveEnd = computeEffectiveEndTime(baseWrapper, initEndTime);

        // 使用 CompletableFuture 收集各分片的统计结果
        List<DateBetween> betweenList = getBetweenList(effectiveStart, effectiveEnd);
        List<CompletableFuture<Integer>> futureCounts = betweenList.stream()
                .map(between -> {
                    QueryWrapper<?> qw = prepareQueryWrapper(baseWrapper, between);
                    return CompletableFuture.supplyAsync(() -> {
                        Integer cnt = countMethod.apply(qw);
                        return cnt == null ? 0 : cnt;
                    }, countExecutor);
                })
                .collect(Collectors.toList());

        // 汇总各分片的计数，最终通过 executor.submit 包装为 Future 返回
        return executor.submit(() -> {
            int total = 0;
            for (CompletableFuture<Integer> f : futureCounts) {
                try {
                    total += f.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new SQLException(e.getCause());
                }
            }
            return total;
        });
    }

    /**
     * 异步分页查询：跨分片计算偏移量、提交各分片查询任务，汇总返回结果列表。
     */
    @Override
    public Future<List<T>> queryListAsync(QueryWrapper<?> wrapper, Integer pageNum, Integer pageSize) {
        // 当 listMethod 或 countMethod 不可用时直接返回空结果
        if (listMethod == null || countMethod == null) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        boolean isQueryAll = (pageSize == 0);
        QueryWrapper<?> baseWrapper = wrapper.clone();
        Date effectiveStart = computeEffectiveStartTime(baseWrapper, initStartTime);
        Date effectiveEnd = computeEffectiveEndTime(baseWrapper, initEndTime);

        return executor.submit(() -> {
            List<DateBetween> betweenList = getBetweenList(effectiveStart, effectiveEnd);
            // 计算跨分片的全局偏移量（即前面页的数据需跳过的记录数）
            int offset = (pageNum - 1) * (isQueryAll ? 0 : pageSize);
            int remaining = pageSize;  // 剩余需要查询的记录数（当 pageSize==0 时不限制）
            List<CompletableFuture<List<T>>> futureList = new ArrayList<>();

            // 遍历各分片顺序查询
            for (DateBetween between : betweenList) {
                QueryWrapper<?> countQw = prepareQueryWrapper(baseWrapper, between);
                int partitionCount = Optional.ofNullable(countMethod.apply(countQw)).orElse(0);
                if (partitionCount == 0) {
                    continue;
                }
                // 如果本分片数据全部在偏移量之前，则更新偏移量后跳过
                if (offset >= partitionCount) {
                    offset -= partitionCount;
                    continue;
                }
                // 确定当前分片查询的数量：
                // 若查询所有数据，则查询当前分片全部数据；
                // 否则，查询 min(剩余记录数, 本分片满足条件的记录数减去 offset)
                int limit = isQueryAll ? partitionCount : Math.min(remaining, partitionCount - offset);
                // 当前分片的查询偏移量
                final int currentOffset = offset;
                // 提交当前分片的查询任务（新克隆一份 QueryWrapper 以避免条件干扰）
                QueryWrapper<?> listQw = prepareQueryWrapper(baseWrapper, between);
                futureList.add(CompletableFuture.supplyAsync(
                        () -> listMethod.apply(listQw, limit, currentOffset),
                        searchExecutor));
                // 后续分片不再需要跳过前面的数据
                offset = 0;
                if (!isQueryAll) {
                    remaining -= limit;
                    if (remaining <= 0) {
                        break;
                    }
                }
            }
            // 汇总所有分片的查询结果
            List<T> result = new ArrayList<>();
            for (CompletableFuture<List<T>> future : futureList) {
                try {
                    List<T> list = future.get();
                    if (list != null) {
                        result.addAll(list);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new SQLException(e.getCause());
                }
            }
            return result;
        });
    }

    @Override
    public PageInfo<T> queryPage(QueryWrapper<?> wrapper, Integer pageNum, Integer pageSize) throws ExecutionException, InterruptedException {
        PageInfo<T> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(pageNum);
        pageInfo.setPageSize(pageSize);
        Future<List<T>> listFuture = queryListAsync(wrapper, pageNum, pageSize);
        Future<Integer> countFuture = queryCountAsync(wrapper);
        pageInfo.setList(listFuture.get());
        pageInfo.setTotal(countFuture.get());
        return pageInfo;
    }

    /* ===================== 辅助方法 ===================== */

    /**
     * 根据传入的 QueryWrapper 和初始时间边界，计算有效的开始时间：
     * 若 QueryWrapper 中有 >= 限制，则取较晚者。
     */
    private Date computeEffectiveStartTime(QueryWrapper<?> queryWrapper, Date initStart) {
        String customSql = queryWrapper.getCustomSqlSegment();
        String geKey = extractKey(customSql, field, ">");
        Date wrapperStart = (geKey != null) ? (Date) queryWrapper.getParamNameValuePairs().get(geKey) : null;
        if (wrapperStart == null) {
            return initStart;
        }
        return (initStart == null || wrapperStart.after(initStart)) ? wrapperStart : initStart;
    }

    /**
     * 根据传入的 QueryWrapper 和初始时间边界，计算有效的结束时间：
     * 若 QueryWrapper 中有 <= 限制，则取较早者。
     */
    private Date computeEffectiveEndTime(QueryWrapper<?> queryWrapper, Date initEnd) {
        String customSql = queryWrapper.getCustomSqlSegment();
        String leKey = extractKey(customSql, field, "<");
        Date wrapperEnd = (leKey != null) ? (Date) queryWrapper.getParamNameValuePairs().get(leKey) : null;
        if (wrapperEnd == null) {
            return initEnd;
        }
        return (initEnd == null || wrapperEnd.before(initEnd)) ? wrapperEnd : initEnd;
    }

    /**
     * 提取 QueryWrapper 中针对指定字段和操作符的参数 key。
     * 例如：若 sql 中包含 "field > xxxPairs{paramKey}" 则返回 paramKey
     */
    private String extractKey(String sql, String field, String operator) {
        String marker = field + " " + operator;
        if (sql != null && sql.contains(marker)) {
            try {
                String sub = sql.substring(sql.indexOf(marker));
                // 假设格式固定，从 "Pairs" 后开始，到 "}" 前结束
                return sub.substring(sub.indexOf("Pairs") + 6, sub.indexOf("}"));
            } catch (Exception e) {
                // 格式不符合预期时返回 null
                return null;
            }
        }
        return null;
    }

    /**
     * 克隆传入的 QueryWrapper，清除其中的 groupBy 条件，并应用当前分片的时间条件。
     */
    private QueryWrapper<?> prepareQueryWrapper(QueryWrapper<?> baseWrapper, DateBetween between) {
        QueryWrapper<?> qw = cloneAndClearGroupBy(baseWrapper);
        applyDateBetweenCondition(qw, between);
        setBetweenParameters(qw, between);
        return qw;
    }

    /**
     * 克隆 QueryWrapper 并清除 groupBy 条件。
     */
    private QueryWrapper<?> cloneAndClearGroupBy(QueryWrapper<?> queryWrapper) {
        QueryWrapper<?> clone = queryWrapper.clone();
        if (clone.getExpression() != null && clone.getExpression().getGroupBy() != null) {
            clone.getExpression().getGroupBy().clear();
        }
        return clone;
    }

    /**
     * 根据分片条件在 QueryWrapper 中添加时间过滤：
     * 当开始时间与结束时间相等时使用 eq，否则使用 ge 与 le。
     */
    private void applyDateBetweenCondition(QueryWrapper<?> queryWrapper, DateBetween between) {
        String customSql = queryWrapper.getCustomSqlSegment();
        String geKey = extractKey(customSql, field, ">");
        String leKey = extractKey(customSql, field, "<");
        if (between.getStartDate().getTime() == between.getEndDate().getTime()) {
            queryWrapper.eq(geKey == null && leKey == null, field, between.getStartDate());
        } else {
            queryWrapper.ge(geKey == null, field, between.getStartDate());
            queryWrapper.le(leKey == null, field, between.getEndDate());
        }
    }

    /**
     * 将分片的起止时间放入 QueryWrapper 参数映射中。
     */
    private void setBetweenParameters(QueryWrapper<?> queryWrapper, DateBetween between) {
        String customSql = queryWrapper.getCustomSqlSegment();
        String geKey = extractKey(customSql, field, ">");
        String leKey = extractKey(customSql, field, "<");
        queryWrapper.getParamNameValuePairs().put(geKey, between.getStartDate());
        queryWrapper.getParamNameValuePairs().put(leKey, between.getEndDate());
    }

    /**
     * 根据有效的开始与结束时间以及分片类型生成分片列表。
     */
    private List<DateBetween> getBetweenList(Date start, Date end) {
        if (start == null || end == null) {
            // 若时间为空，从配置中取默认值
            ShardingPropertics props = SpringContextUtil.getBean(ShardingPropertics.class);
            if (start == null) {
                start = (props.getMinDate() != null ? props.getMinDate() : DateUtils.parse("2000-01-01 00:00:00"));
            }
            if (end == null) {
                end = new Date();
            }
        }
        switch (shardingType) {
            case Year:
                return betweenDateByYears(start, end);
            case Month:
                return betweenDateByMonths(start, end);
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 按年份生成时间分片列表
     */
    private static List<DateBetween> betweenDateByYears(Date startTime, Date endTime) {
        List<DateBetween> dateRanges = new ArrayList<>();
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startTime);
        int startYear = startCal.get(Calendar.YEAR);

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endTime);
        int endYear = endCal.get(Calendar.YEAR);

        // 按年份倒序生成分片，保证分页时先查询最新年份
        for (int year = endYear; year >= startYear; year--) {
            DateBetween between = new DateBetween();
            Calendar cal = Calendar.getInstance();
            if (year == startYear) {
                between.setStartDate(startTime);
            } else {
                cal.set(year, Calendar.JANUARY, 1, 0, 0, 0);
                between.setStartDate(cal.getTime());
            }
            if (year == endYear) {
                between.setEndDate(endTime);
            } else {
                cal.set(year, Calendar.DECEMBER, 31, 23, 59, 59);
                between.setEndDate(cal.getTime());
            }
            dateRanges.add(between);
        }
        return dateRanges;
    }

    /**
     * 按月份生成时间分片列表
     */
    private static List<DateBetween> betweenDateByMonths(Date startTime, Date endTime) {
        List<DateBetween> dateRanges = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(startTime);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endTime);

        while (cal.before(endCal)) {
            DateBetween between = new DateBetween();
            between.setStartDate(cal.getTime());

            // 设置当前月份的最后一天 23:59:59
            Calendar temp = (Calendar) cal.clone();
            temp.set(Calendar.DAY_OF_MONTH, temp.getActualMaximum(Calendar.DAY_OF_MONTH));
            temp.set(Calendar.HOUR_OF_DAY, 23);
            temp.set(Calendar.MINUTE, 59);
            temp.set(Calendar.SECOND, 59);
            Date monthEnd = temp.getTime();
            between.setEndDate(monthEnd.after(endTime) ? endTime : monthEnd);
            dateRanges.add(between);

            // 移动到下个月初
            cal.add(Calendar.MONTH, 1);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
        }
        return dateRanges;
    }

    /**
     * 内部类，表示时间分片的起止时间
     */
    @Data
    private static class DateBetween {
        private Date startDate;
        private Date endDate;

        public DateBetween() { }

        public DateBetween(Date startDate, Date endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
}

