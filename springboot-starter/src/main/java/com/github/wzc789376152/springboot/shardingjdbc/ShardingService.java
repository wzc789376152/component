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
import org.springframework.scheduling.annotation.AsyncResult;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 分页搜索服务类
 * 不支持group by查询
 */
public class ShardingService<T> implements IShardingService<T> {
    private final ExecutorService executor;
    private final ExecutorService searchExecutor;
    private final ExecutorService countExecutor;
    private final ShardingCountFunction<Wrapper, Integer> countMethod;
    private final ShardingListFunction<Wrapper, Integer, Integer, List<T>> listMethod;
    private final String field;
    private Date startTime;
    private Date endTime;

    private final ShardingType shardingType;

    public ShardingService(String field, Date startTime, Date endTime, ShardingCountFunction<Wrapper, Integer> countMethod, ShardingListFunction<Wrapper, Integer, Integer, List<T>> listMethod, String asyncName, String searchAsyncName, String countAsyncName, ShardingType shardingType) {
        this.field = field;
        this.startTime = startTime;
        this.endTime = endTime;
        this.countMethod = countMethod;
        this.listMethod = listMethod;
        this.shardingType = shardingType;
        executor = SpringContextUtil.getBean(asyncName, ExecutorService.class);
        countExecutor = SpringContextUtil.getBean(countAsyncName, ExecutorService.class);
        searchExecutor = SpringContextUtil.getBean(searchAsyncName, ExecutorService.class);
    }

    @Override
    public Integer queryCount(QueryWrapper<?> wrapper) throws ExecutionException, InterruptedException {
        return queryCountAsync(wrapper).get();
    }

    @Override
    public List<T> queryList(QueryWrapper<?> wrapper, Integer pageNum, Integer pageSize) throws ExecutionException, InterruptedException {
        return queryListAsync(wrapper, pageNum, pageSize).get();
    }

    @Override
    public Future<Integer> queryCountAsync(QueryWrapper<?> wrapper) {
        // 克隆并调整时间边界（同时更新全局 startTime、endTime）
        QueryWrapper<?> queryWrapper = wrapper.clone();
        adjustTimeBoundary(queryWrapper);

        return executor.submit(() -> {
            List<DateBetween> betweenList = getBetweenList(startTime, endTime);
            int totalCount = 0;
            List<Future<Integer>> futureList = new ArrayList<>();

            // 针对每个时间分片异步统计
            for (DateBetween between : betweenList) {
                QueryWrapper<?> qw = cloneAndClearGroupBy(queryWrapper);
                applyDateBetweenCondition(qw, between);
                setBetweenParameters(qw, between);

                futureList.add(countExecutor.submit(() -> {
                    Integer cnt = countMethod.apply(qw);
                    return cnt == null ? 0 : cnt;
                }));
            }

            // 汇总各分片的计数
            for (Future<Integer> future : futureList) {
                try {
                    totalCount += future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new SQLException(e.getCause());
                }
            }
            return totalCount;
        });
    }

    @Override
    public Future<List<T>> queryListAsync(QueryWrapper<?> wrapper, Integer pageNum, Integer pageSize) {
        if (listMethod == null || countMethod == null) {
            return AsyncResult.forValue(new ArrayList<>());
        }
        boolean isAll = (pageSize == 0);
        // 使用 AtomicInteger 表示剩余需查询条数（便于在多分片间累减）
        AtomicInteger remainingSize = new AtomicInteger(pageSize);
        QueryWrapper<?> queryWrapper = wrapper.clone();
        adjustTimeBoundary(queryWrapper);

        return executor.submit(() -> {
            List<DateBetween> betweenList = getBetweenList(startTime, endTime);
            // 计算跨分片的起始偏移量（例如第 pageNum 页前的数据要跳过）
            int beginSize = (pageNum - 1) * remainingSize.get();
            List<Future<List<T>>> futureList = new ArrayList<>();
            boolean isEnd = false;

            for (DateBetween between : betweenList) {
                QueryWrapper<?> qw = cloneAndClearGroupBy(queryWrapper);
                applyDateBetweenCondition(qw, between);
                setBetweenParameters(qw, between);

                Integer countObj = countMethod.apply(qw);
                int count = (countObj == null ? 0 : countObj);
                if (count == 0) {
                    continue;
                }
                // 如果本分片的数据全部在分页起点之前，则跳过，并减少偏移量
                if (beginSize >= count) {
                    beginSize -= count;
                    continue;
                }
                if (isEnd) {
                    break;
                }

                // 当前分片需要查询的条数
                int limit = remainingSize.get();
                if (limit == 0) {
                    limit = count;
                }
                if (count - beginSize < remainingSize.get()) {
                    limit = count - beginSize;
                }

                QueryWrapper<?> listQw = cloneAndClearGroupBy(queryWrapper);
                applyDateBetweenCondition(listQw, between);
                setBetweenParameters(listQw, between);

                final Integer finalLimit = limit;
                final Integer finalBeginSize = beginSize;
                futureList.add(searchExecutor.submit(() ->
                        listMethod.apply(listQw, finalLimit, finalBeginSize)));

                // 后续分片查询从头开始（起始偏移归零）
                beginSize = 0;
                if (!isAll) {
                    // 若当前分片查询数小于 pageSize，则剩余数减去已查询的记录，否则结束查询
                    if (remainingSize.get() == 0 || finalLimit == remainingSize.get()) {
                        isEnd = true;
                    } else {
                        remainingSize.addAndGet(-finalLimit);
                    }
                }
            }

            // 汇总所有分片返回的数据
            List<T> result = new ArrayList<>();
            for (Future<List<T>> future : futureList) {
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

    /* ================= 辅助方法 ================= */

    /**
     * 根据传入的 QueryWrapper 调整全局的起止时间
     */
    private void adjustTimeBoundary(QueryWrapper<?> queryWrapper) {
        String expressBaseStr = queryWrapper.getCustomSqlSegment();
        String geBaseKey = geKey(expressBaseStr, field);
        String leBaseKey = leKey(expressBaseStr, field);
        if (geBaseKey != null) {
            Date beginTime = (Date) queryWrapper.getParamNameValuePairs().get(geBaseKey);
            if (startTime == null || beginTime.getTime() > startTime.getTime()) {
                startTime = beginTime;
            }
        }
        if (leBaseKey != null) {
            Date finalTime = (Date) queryWrapper.getParamNameValuePairs().get(leBaseKey);
            if (endTime == null || finalTime.getTime() < endTime.getTime()) {
                endTime = finalTime;
            }
        }
    }

    /**
     * 克隆 QueryWrapper 并清空其中的 groupBy 条件
     */
    private QueryWrapper<?> cloneAndClearGroupBy(QueryWrapper<?> queryWrapper) {
        QueryWrapper<?> clone = queryWrapper.clone();
        if (clone.getExpression() != null && clone.getExpression().getGroupBy() != null) {
            clone.getExpression().getGroupBy().clear();
        }
        return clone;
    }

    /**
     * 根据分片条件对 QueryWrapper 应用时间条件（当开始时间与结束时间相等时采用 eq，否则用 ge 与 le）
     */
    private void applyDateBetweenCondition(QueryWrapper<?> queryWrapper, DateBetween between) {
        String expressStr = queryWrapper.getCustomSqlSegment();
        String geKey = geKey(expressStr, field);
        String leKey = leKey(expressStr, field);
        if (between.getStartDate().getTime() == between.getEndDate().getTime()) {
            queryWrapper.eq(geKey == null && leKey == null, field, between.getStartDate());
        } else {
            queryWrapper.ge(geKey == null, field, between.getStartDate());
            queryWrapper.le(leKey == null, field, between.getEndDate());
        }
    }

    /**
     * 将分片的起止时间放入 QueryWrapper 的参数映射中
     */
    private void setBetweenParameters(QueryWrapper<?> queryWrapper, DateBetween between) {
        String expressStr = queryWrapper.getCustomSqlSegment();
        String geKey = geKey(expressStr, field);
        String leKey = leKey(expressStr, field);
        queryWrapper.getParamNameValuePairs().put(geKey, between.getStartDate());
        queryWrapper.getParamNameValuePairs().put(leKey, between.getEndDate());
    }

    /**
     * 根据全局 startTime 和 endTime 以及分片类型生成分片列表
     */
    private List<DateBetween> getBetweenList(Date start, Date end) {
        List<DateBetween> betweenList;
        switch (shardingType) {
            case Year:
                betweenList = betweenDateByYears(start, end);
                break;
            case Month:
                betweenList = betweenDateByMonths(start, end);
                break;
            default:
                betweenList = Collections.emptyList();
        }
        return betweenList;
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

    private String leKey(String sql, String field) {
        boolean hasField = sql.contains(field + " <");
        if (hasField) {
            String key = sql.substring(sql.indexOf(field + " <"));
            key = key.substring(key.indexOf("Pairs") + 6, key.indexOf("}"));
            return key;
        }
        return null;
    }

    private String geKey(String sql, String field) {
        boolean hasField = sql.contains(field + " >");
        if (hasField) {
            String key = sql.substring(sql.indexOf(field + " >"));
            key = key.substring(key.indexOf("Pairs") + 6, key.indexOf("}"));
            return key;
        }
        return null;
    }

    @Data
    private static class DateBetween {
        private Date startDate;
        private Date endDate;

        public DateBetween() {
        }

        public DateBetween(Date startDate, Date endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    private static List<DateBetween> betweenDateByYears(Date startTime, Date endTime) {
        if (startTime == null) {
            ShardingPropertics shardingPropertics = SpringContextUtil.getBean(ShardingPropertics.class);
            startTime = shardingPropertics.getMinDate() != null ? shardingPropertics.getMinDate() : DateUtils.parse("2000-01-01 00:00:00");
        }
        if (endTime == null) {
            endTime = new Date();
        }

        List<DateBetween> dateRanges = new ArrayList<>();
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startTime);
        int startYear = startCal.get(Calendar.YEAR);

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endTime);
        int endYear = endCal.get(Calendar.YEAR);

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

    private static List<DateBetween> betweenDateByMonths(Date startTime, Date endTime) {
        if (startTime == null) {
            ShardingPropertics shardingPropertics = SpringContextUtil.getBean(ShardingPropertics.class);
            startTime = shardingPropertics.getMinDate() != null ? shardingPropertics.getMinDate() : DateUtils.parse("2000-01-01 00:00:00");
        }
        if (endTime == null) {
            endTime = new Date();
        }

        if (startTime.equals(endTime)) {
            return Collections.singletonList(new DateBetween(startTime, endTime));
        }

        List<DateBetween> dateRanges = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(startTime);

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endTime);

        while (cal.before(endCal)) {
            DateBetween between = new DateBetween();
            between.setStartDate(cal.getTime());

            // 设置当前月份的最后一天 23:59:59
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);

            if (cal.getTime().after(endTime)) {
                between.setEndDate(endTime);
            } else {
                between.setEndDate(cal.getTime());
            }

            dateRanges.add(between);

            // 移动到下个月
            cal.add(Calendar.MONTH, 1);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
        }

        return dateRanges;
    }
}
