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

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
    public Future<Integer> queryCountAsync(QueryWrapper<?> wrapper) {
        QueryWrapper<?> queryWrapper = wrapper.clone();
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
        return executor.submit(() -> {
            List<DateBetween> betweenList = null;
            switch (shardingType) {
                case Year:
                    betweenList = betweenDateByYears(startTime, endTime);
                    break;
                case Month:
                    betweenList = betweenDateByMonths(startTime, endTime);
            }
            int result = 0;
            List<Future<Integer>> futureList = new ArrayList<>();
            for (DateBetween between : betweenList) {
                QueryWrapper<?> queryWrapper1 = queryWrapper.clone();
                if (queryWrapper1.getExpression().getGroupBy() != null && queryWrapper1.getExpression().getGroupBy().size() > 0) {
                    for (int j = 0; j < queryWrapper1.getExpression().getGroupBy().size(); j++) {
                        queryWrapper1.getExpression().getGroupBy().remove(j);
                    }
                }
                String expressStr = queryWrapper1.getCustomSqlSegment();
                String geKey = geKey(expressStr, field);
                String leKey = leKey(expressStr, field);
                if (between.getStartDate().getTime() == between.getEndDate().getTime()) {
                    queryWrapper1.eq(geKey == null && leKey == null, field, between.getStartDate());
                } else {
                    queryWrapper1.le(leKey == null, field, between.getEndDate());
                    queryWrapper1.ge(geKey == null, field, between.getStartDate());
                }
                expressStr = queryWrapper1.getCustomSqlSegment();
                geKey = geKey(expressStr, field);
                leKey = leKey(expressStr, field);
                queryWrapper1.getParamNameValuePairs().put(geKey, between.getStartDate());
                queryWrapper1.getParamNameValuePairs().put(leKey, between.getEndDate());
                futureList.add(countExecutor.submit(() -> countMethod.apply(queryWrapper1)));
            }
            for (Future<Integer> future1 : futureList) {
                try {
                    Integer obj = future1.get();
                    if (obj == null) {
                        obj = 0;
                    }
                    result += obj;
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            return result;
        });
    }

    @Override
    public List<T> queryList(QueryWrapper<?> wrapper, Integer pageNum, Integer pageSize) throws ExecutionException, InterruptedException {
        return queryListAsync(wrapper, pageNum, pageSize).get();
    }

    @Override
    public Future<List<T>> queryListAsync(QueryWrapper<?> wrapper, Integer pageNum, Integer pageSize) {
        if (listMethod == null || countMethod == null) {
            return AsyncResult.forValue(new ArrayList<>());
        }
        final Integer[] size = {pageSize};
        QueryWrapper<?> queryWrapper = wrapper.clone();
        String expressBaseStr = queryWrapper.getCustomSqlSegment();
        String geBaseKey = geKey(expressBaseStr, field);
        String leBaseKey = leKey(expressBaseStr, field);
        if (geBaseKey != null) {
            Date beginTime = (Date) queryWrapper.getParamNameValuePairs().get(geBaseKey);
            if (beginTime.getTime() > startTime.getTime()) {
                startTime = beginTime;
            }
        }
        if (leBaseKey != null) {
            Date finalTime = (Date) queryWrapper.getParamNameValuePairs().get(leBaseKey);
            if (finalTime.getTime() < endTime.getTime()) {
                endTime = finalTime;
            }
        }
        return executor.submit(() -> {
            //按照时间分片
            List<DateBetween> betweenList = null;
            switch (shardingType) {
                case Year:
                    betweenList = betweenDateByYears(startTime, endTime);
                    break;
                case Month:
                    betweenList = betweenDateByMonths(startTime, endTime);
            }
            //查询起点
            int beginSize = (pageNum - 1) * size[0];
            int i = 0;
            Map<Integer, Future<List<T>>> resultMap = new HashMap<>();
            boolean isEnd = false;
            //查询每个分片实际数据，多线程
            for (DateBetween between : betweenList) {
                QueryWrapper<?> queryWrapper2 = queryWrapper.clone();
                if (queryWrapper2.getExpression().getGroupBy() != null && queryWrapper2.getExpression().getGroupBy().size() > 0) {
                    for (int j = 0; j < queryWrapper2.getExpression().getGroupBy().size(); j++) {
                        queryWrapper2.getExpression().getGroupBy().remove(j);
                    }
                }
                String expressStr = queryWrapper2.getCustomSqlSegment();
                String geKey = geKey(expressStr, field);
                String leKey = leKey(expressStr, field);
                if (between.getStartDate().getTime() == between.getEndDate().getTime()) {
                    queryWrapper2.eq(geKey == null && leKey == null, field, between.getStartDate());
                } else {
                    queryWrapper2.le(leKey == null, field, between.getEndDate());
                    queryWrapper2.ge(geKey == null, field, between.getStartDate());
                }
                expressStr = queryWrapper2.getCustomSqlSegment();
                geKey = geKey(expressStr, field);
                leKey = leKey(expressStr, field);
                queryWrapper2.getParamNameValuePairs().put(geKey, between.getStartDate());
                queryWrapper2.getParamNameValuePairs().put(leKey, between.getEndDate());
                Integer object = countMethod.apply(queryWrapper2);
                int count = Integer.parseInt(object == null ? "0" : object.toString());
                if (count == 0L) {
                    i++;
                    continue;
                }
                if (beginSize >= count) {
                    //如果查询的起点是比这张表的数据大的，就忽略这张表，查询下一张表，并把起点位置去掉该表的总数
                    beginSize -= count;
                } else {
                    if (isEnd) {
                        break;
                    }
                    Integer limit = size[0];
                    if (limit == 0) {
                        limit = count;
                    }
                    //判断查询的条数是否足够，如果不足够，则查询该表实际数值。如需查询20条，但是该表只有15条
                    if (count - beginSize < size[0]) {
                        limit = count - beginSize;
                    }
                    Integer finalBeginSize = beginSize;
                    Integer finalLimit = limit;
                    QueryWrapper<?> queryWrapper1 = queryWrapper.clone();
                    expressStr = queryWrapper1.getCustomSqlSegment();
                    geKey = geKey(expressStr, field);
                    leKey = leKey(expressStr, field);
                    if (between.getStartDate().getTime() == between.getEndDate().getTime()) {
                        queryWrapper1.eq(geKey == null && leKey == null, field, between.getStartDate());
                    } else {
                        queryWrapper1.le(leKey == null, field, between.getEndDate());
                        queryWrapper1.ge(geKey == null, field, between.getStartDate());
                    }
                    expressStr = queryWrapper1.getCustomSqlSegment();
                    geKey = geKey(expressStr, field);
                    leKey = leKey(expressStr, field);
                    queryWrapper1.getParamNameValuePairs().put(geKey, between.getStartDate());
                    queryWrapper1.getParamNameValuePairs().put(leKey, between.getEndDate());
                    Future<List<T>> future = searchExecutor.submit(() -> listMethod.apply(queryWrapper1, finalLimit, finalBeginSize));
                    resultMap.put(i, future);
                    beginSize = 0;
                    if (size[0] == 0 || limit.equals(size[0])) {
                        isEnd = true;
                    } else {
                        //查询完成将查询的条目减掉，如需查询20条，该表只查了15条，下次查询则只需查询5条
                        size[0] = size[0] - limit;
                    }
                }
                i++;
            }
            //将查询数据汇总
            List<T> result = new ArrayList<>();
            for (int j = 0; j < i; j++) {
                try {
                    Future<List<T>> future = resultMap.get(j);
                    if (future == null) {
                        continue;
                    }
                    List<T> o = future.get();
                    result.addAll(o);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            Date endDate = new Date();
//            System.out.println("查询耗时：" + (endDate.getTime() - beginDate.getTime()) / 1000 + "s");
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
    }

    private static List<DateBetween> betweenDateByYears(Date startTime, Date endTime) {
        if (startTime == null) {
            ShardingPropertics shardingPropertics = SpringContextUtil.getBean(ShardingPropertics.class);
            if (shardingPropertics.getMinDate() != null) {
                startTime = shardingPropertics.getMinDate();
            } else {
                startTime = DateUtils.parse("2000-01-01 00:00:00");
            }
        }
        if (endTime == null) {
            endTime = new Date();
        }
        List<DateBetween> list = new LinkedList<>();
        int startYear = Integer.parseInt(DateUtils.format(startTime, "yyyy"));
        int endYear = Integer.parseInt(DateUtils.format(endTime, "yyyy"));
        for (int i = endYear; i >= startYear; i--) {
            if (i == endYear) {
                DateBetween between = new DateBetween();
                if (i == startYear) {
                    between.setStartDate(startTime);
                } else {
                    between.setStartDate(DateUtils.parse(i + "-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"));
                }
                between.setEndDate(endTime);
                list.add(between);
            } else if (i == startYear) {
                DateBetween between = new DateBetween();
                between.setEndDate(DateUtils.parse(i + "-12-31 23:59:59", "yyyy-MM-dd HH:mm:ss"));
                between.setStartDate(startTime);
                list.add(between);
            } else {
                DateBetween between = new DateBetween();
                between.setEndDate(DateUtils.parse(i + "-12-31 23:59:59", "yyyy-MM-dd HH:mm:ss"));
                between.setStartDate(DateUtils.parse(i + "-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"));
                list.add(between);
            }
        }
        return list;
    }

    private static List<DateBetween> betweenDateByMonths(Date startTime, Date endTime) {
        if (startTime == null) {
            ShardingPropertics shardingPropertics = SpringContextUtil.getBean(ShardingPropertics.class);
            if (shardingPropertics.getMinDate() != null) {
                startTime = shardingPropertics.getMinDate();
            } else {
                startTime = DateUtils.parse("2000-01-01 00:00:00");
            }
        }
        if (endTime == null) {
            endTime = new Date();
        }
        if (startTime.getTime() == endTime.getTime()) {
            List<DateBetween> betweenList = new ArrayList<>();
            DateBetween between = new DateBetween();
            between.setStartDate(startTime);
            between.setEndDate(endTime);
            betweenList.add(between);
            return betweenList;
        }
        List<DateBetween> list = new LinkedList<>();
        Date d1 = startTime;// 定义起始日期
        Date d2 = endTime;// 定义结束日期
        Calendar dd = Calendar.getInstance();// 定义日期实例
        dd.setTime(d1);// 设置日期起始时间
        Calendar cale = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.setTime(d2);
        int startDay = dd.get(Calendar.DAY_OF_MONTH);
        int endDay = c.get(Calendar.DAY_OF_MONTH);
        DateBetween keyValueForDate = null;
        while (dd.getTime().before(d2)) {// 判断是否到结束日期
            keyValueForDate = new DateBetween();
            cale.setTime(dd.getTime());
            if (dd.getTime().equals(d1)) {
                cale.set(Calendar.DAY_OF_MONTH, dd.getActualMaximum(Calendar.DAY_OF_MONTH));
                keyValueForDate.setStartDate(d1);
                Date finishDate = cale.getTime();
                if (finishDate.before(d2)) {
                    cale.set(Calendar.HOUR_OF_DAY, 23);
                    cale.set(Calendar.MINUTE, 59);
                    cale.set(Calendar.SECOND, 59);
                    keyValueForDate.setEndDate(cale.getTime());
                } else {
                    keyValueForDate.setEndDate(d2);
                }

            } else if (dd.get(Calendar.MONTH) == c.get(Calendar.MONTH) && dd.get(Calendar.YEAR) == c.get(Calendar.YEAR)) {
                cale.set(Calendar.DAY_OF_MONTH, 1);//取第一天
                keyValueForDate.setStartDate(cale.getTime());
                keyValueForDate.setEndDate(d2);
            } else {
                cale.set(Calendar.DAY_OF_MONTH, 1);//取第一天
                keyValueForDate.setStartDate(cale.getTime());
                cale.set(Calendar.DAY_OF_MONTH, dd
                        .getActualMaximum(Calendar.DAY_OF_MONTH));
                cale.set(Calendar.HOUR_OF_DAY, 23);
                cale.set(Calendar.MINUTE, 59);
                cale.set(Calendar.SECOND, 59);
                keyValueForDate.setEndDate(cale.getTime());

            }
            list.add(keyValueForDate);
            dd.add(Calendar.MONTH, 1);// 进行当前日期月份加1

        }
        if (endDay < startDay) {
            keyValueForDate = new DateBetween();

            cale.setTime(d2);
            cale.set(Calendar.DAY_OF_MONTH, 1);//取第一天
            cale.set(Calendar.HOUR_OF_DAY, 0);
            cale.set(Calendar.MINUTE, 0);
            cale.set(Calendar.SECOND, 0);
            keyValueForDate.setStartDate(cale.getTime());
            keyValueForDate.setEndDate(d2);
            list.add(keyValueForDate);
        }
        return list.stream().sorted(Comparator.comparing(DateBetween::getStartDate).reversed()).collect(Collectors.toList());
    }
}
