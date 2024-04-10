package com.github.wzc789376152.springboot.utils;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.github.wzc789376152.springboot.config.SpringContextUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DatabaseUtils {
    private static JdbcTemplate jdbcTemplate;
    //查库名称
    private static final String schemaSql = "SELECT DATABASE()";
    //查表
    private static final String tableCountSql = "SELECT COUNT(1) FROM information_schema.TABLES WHERE TABLE_SCHEMA = '%s' AND TABLE_NAME = '%s';";

    //查索引
    private static final String statisticsSql = "select TABLE_SCHEMA,TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX,COLUMN_NAME,NON_UNIQUE,INDEX_TYPE from information_schema.statistics where table_schema = '%s' and table_name = '%s';";

    private static final String columnSql = "select table_schema,table_name,column_name,column_type,column_comment,data_type,column_default,is_nullable,extra from information_schema.columns where table_schema = '%s' and table_name ='%s'";
    // 修改列的sql模版
    private static final String MODIFY_COLUMN = "ALTER TABLE %s.%s MODIFY COLUMN %s %s %s %s %s %s";
    // 新增列的sql模版
    private static final String ADD_COLUMN = "ALTER TABLE %s.%s ADD COLUMN %s %s %s %s %s %s";

    private static final String DELETE_COLUMN = "ALTER TABLE %s.%s DROP COLUMN %s";

    // 添加索引的模版（区分唯一/不唯一索引）
    // ALTER TABLE %s.%s ADD %s INDEX %s (列名)
    // ALTER TABLE %s.%s ADD PRIMARY KEY (id);
    // ALTER TABLE %s.%s ADD FULLTEXT xxx(NAME);
    private static final String ADD_INDEX = "ALTER TABLE %s.%s ADD %s %s (%s)";

    // 索引没有修改，只能先删除，再添加
    private static final String DROP_INDEX = "ALTER TABLE %s.%s DROP INDEX %s";

    @Data
    public static class ColumnDO {
        private String columnName;
        private String columnType;
        private String columnComment;
        private String dataType;
        private String columnDefault;
        private String isNullable;
        private String extra;
        // 自己添加的属性，这个列是否为新增（还是修改）
        private boolean isAdd;

        private boolean isDelete;
    }

    @Data
    @Builder
    public static class StatisticsDTO {
        private String indexName;
        private List<String> columns;
        private int nonUnique;
        private String indexType;
    }


    @Data
    public static class StatisticsDO {
        private String indexName;
        private int seqInIndex;
        private String columnName;
        private int nonUnique;
        private String indexType;

    }

    public static JdbcTemplate getJdbcTemplate() {
        if (jdbcTemplate == null) {
            JdbcTemplate jdbcTemplate1 = SpringContextUtil.getBean(JdbcTemplate.class);
            if (jdbcTemplate1 != null) {
                DynamicRoutingDataSource dynamicRoutingDataSource = SpringContextUtil.getBean(DynamicRoutingDataSource.class);
                DataSource dataSource = dynamicRoutingDataSource.getDataSource("master");
                if (dataSource instanceof ShardingSphereDataSource) {
                    PooledDataSource pooledDataSource = new PooledDataSource();
                    Environment environment = SpringContextUtil.getApplicationContext().getEnvironment();
                    String defultName = environment.getProperty("spring.shardingsphere.sharding.default-data-source-name");
                    String datasourcePropertyStr = "spring.shardingsphere.datasource." + defultName;
                    pooledDataSource.setUrl(environment.getProperty(datasourcePropertyStr + ".url"));
                    pooledDataSource.setUsername(environment.getProperty(datasourcePropertyStr + ".username"));
                    pooledDataSource.setPassword(environment.getProperty(datasourcePropertyStr + ".password"));
                    pooledDataSource.setDriver(environment.getProperty(datasourcePropertyStr + ".driver-class-name"));
                    jdbcTemplate = new JdbcTemplate(pooledDataSource);
                } else {
                    jdbcTemplate = jdbcTemplate1;
                }
            }
        }
        return jdbcTemplate;
    }

    /**
     * 同步表结构
     */
    public static void syncTable(String table, String sourceTable) {
        //查当前库名
        String schemaName = getJdbcTemplate().queryForObject(schemaSql, String.class);
        Integer tableCount = getJdbcTemplate().queryForObject(String.format(tableCountSql, schemaName, table), Integer.class);
        if (tableCount == null || tableCount == 0) {
            return;
        }
        Integer sourceTableCount = getJdbcTemplate().queryForObject(String.format(tableCountSql, schemaName, sourceTable), Integer.class);
        if (sourceTableCount == null || sourceTableCount == 0) {
            return;
        }
        log.info("开始同步表" + table);
        Date startTime = new Date();
        syncColumn(schemaName, table, sourceTable);
        syncStatistics(schemaName, table, sourceTable);
        log.info("结束同步表" + table + ",耗时：" + (new Date().getTime() - startTime.getTime()));
    }

    // 同步列
    private static void syncColumn(String schemaName, String table, String sourceTable) {
        // 1、获取，在src原 数据库实例下库的表的结构（字段+索引）
        List<ColumnDO> srcColumns = getJdbcTemplate().query(String.format(columnSql, schemaName, sourceTable), new BeanPropertyRowMapper<>(ColumnDO.class));
        // 2、获取，在dst目标 数据库实例下库的表的结构（字段+索引）
        List<ColumnDO> dstColumnDOS = getJdbcTemplate().query(String.format(columnSql, schemaName, table), new BeanPropertyRowMapper<>(ColumnDO.class));
        // 1与2 实现的功能是一摸一样的，只不过2这里又封装了一下。
        // 3、diff 差异
        List<ColumnDO> columnDOS = diffColumn(srcColumns, dstColumnDOS);
        // 4、基于差异，生成sql
        List<String> sqls = generateSql(schemaName, table, columnDOS);
        for (String sql : sqls) {
            // 5、执行sql
            try {
                getJdbcTemplate().execute(sql);
            } catch (Exception e) {
                log.error("执行SQL异常：" + sql, e);
            }
        }
    }

    // 做差集，使用第三方包guava
    private static List<ColumnDO> diffColumn(List<ColumnDO> srcColumns, List<ColumnDO> dstColumnDOS) {
        // 1、区分列的是实体类，是新增的还是修改的
        // 如何判断是新增的：列的名字不一致，就是新增的

        // 将List转成Set集合,然后求差值
        Set<ColumnDO> diffColumns = Sets.difference(new HashSet<>(srcColumns), new HashSet<>(dstColumnDOS)).immutableCopy();

        // 将src 列的集合，每个实体的类的名字组合成一个集合
        Set<String> srcNames = srcColumns.stream().map(ColumnDO::getColumnName).collect(Collectors.toSet());
        // 将src 列的集合，每个实体的类的名字组合成一个集合
        Set<String> dstNames = dstColumnDOS.stream().map(ColumnDO::getColumnName).collect(Collectors.toSet());
        // 将src 列名字的集合与dst列名字的集合求差值.判断哪些列是新增的
        Set<String> addNames = Sets.difference(new HashSet<>(srcNames), new HashSet<>(dstNames)).immutableCopy();
        Set<String> deleteNames = Sets.difference(new HashSet<>(dstNames), new HashSet<>(srcNames)).immutableCopy();
        // 给Column 设置 isAdd。
        Set<ColumnDO> columnDOList = diffColumns.stream()
                .peek(columnDO -> {
                    if (addNames.contains(columnDO.getColumnName())) {
                        columnDO.setAdd(true);
                    }
                }).collect(Collectors.toSet());
        for (ColumnDO columnDO : dstColumnDOS) {
            if (deleteNames.contains(columnDO.getColumnName())) {
                columnDO.setDelete(true);
                columnDOList.add(columnDO);
            }
        }
        return new ArrayList<>(columnDOList);
    }

    private static List<String> generateSql(String schemaName, String tableName, List<ColumnDO> columnDOS) {
        // ALTER TABLE Student MODIFY COLUMN id VARCHAR(32) NOT NULL DEFAULT "000" COMMENT '备注';
        // ALTER TABLE %s.%s MODIFY COLUMN %s %s %s %s %s;
        // 将ColumnDo 的list 转成 String 语句的list，转换型，使用map
        return columnDOS.stream()
                .map(columnDO -> {
//                    String sqlModel;
//                    if (columnDo.isAdd()){
//                        sqlModel = SqlModel.ADD_COLUMN;
//                    } else {
//                        sqlModel = SqlModel.MODIFY_COLUMN;
//                    }
                    if (columnDO.isDelete()) {
                        return String.format(DELETE_COLUMN, schemaName, tableName, columnDO.getColumnName());
                    } else {
                        return String.format(columnDO.isAdd() ? ADD_COLUMN : MODIFY_COLUMN,
                                schemaName, //库名
                                tableName, // 表名
                                columnDO.getColumnName(),// 列名
                                columnDO.getColumnType(),// 列的类型
                                nullableSet(columnDO.getIsNullable()),// 列是否为空
                                defaultSet(columnDO.getColumnDefault()),// 列的默认值设置
                                getExtra(columnDO.getExtra()),
                                commentSet(columnDO.getColumnComment())// 设置列的备注
                        );
                    }
                }).collect(Collectors.toList());
    }

    // 列是否为空的处理
    private static String nullableSet(String nullable) {
        if ("NO".equals(nullable)) {
            return "not null";
        }
        return "null";
    }

    // 列，默认值的处理，（可能为空，null，0）
    private static String defaultSet(String defaultValue) {
        if (Objects.isNull(defaultValue)) {
            return "";
        }
        return "DEFAULT " + defaultValue;
    }

    private static String getExtra(String extra) {
        return extra != null ? extra.replaceAll("DEFAULT_GENERATED", "") : "";
    }

    // 列，备注的处理
    private static String commentSet(String comment) {
        if (Objects.isNull(comment)) {
            return "";
        }
        return "COMMENT '" + comment + "'";
    }

    //
    private static String indexTypeSet(StatisticsDTO dto) {
        // 1、主键 index_name 为 PRIMARY
        // 2、唯一索引 unique = 0 并且 index_name != PRIMARY
        // 3、普通索引 unique = 1 并且 index_type = BTREE
        // 4、全文索引 unique = 1 并且 index_type = FULLTEXT

        if (("PRIMARY").equals(dto.getIndexName())) {
            return "PRIMARY KEY";
        }
        if (dto.getNonUnique() == 0) {
            return "UNIQUE";
        }
        if ("BTREE".equals(dto.getIndexType())) {
            return "INDEX";
        } else {
            return "FULLTEXT";
        }
    }

    // 同步索引
    private static void syncStatistics(String schemaName, String tableName, String sourceTable) {
        // 1、获取，在src原 数据库实例下库的表的结构（字段+索引）
        List<StatisticsDO> srcStatisticDos = getJdbcTemplate().query(String.format(statisticsSql, schemaName, sourceTable), new BeanPropertyRowMapper<>(StatisticsDO.class));
        // 2、获取，在dst目标 数据库实例下库的表的结构（字段+索引）
        List<StatisticsDO> dstStatisticDos = getJdbcTemplate().query(String.format(statisticsSql, schemaName, tableName), new BeanPropertyRowMapper<>(StatisticsDO.class));
        // 3、diff 差异
        Map<Boolean, List<StatisticsDTO>> diffMap = diffStatistics(srcStatisticDos, dstStatisticDos);
        Map<Boolean, List<StatisticsDTO>> diffMap1 = diffStatistics(dstStatisticDos, srcStatisticDos);

        // 4、基于差异，生成sql
        List<String> addSqls = generateAddIndex(schemaName, tableName, diffMap.get(true));
        // 删除的索引
        List<String> modifyDropSqls = generateDropIndex(schemaName, tableName, diffMap.get(false));
        List<String> dropSqls = generateDropIndex(schemaName, tableName, diffMap1.get(true));
        if (!dropSqls.isEmpty()) {
            modifyDropSqls.addAll(dropSqls);
        }
        // 再增加
        List<String> modifyCreateSqls = generateAddIndex(schemaName, tableName, diffMap.get(false));
        // 5、执行sql
        for (String sql : addSqls) {
            try {
                getJdbcTemplate().execute(sql);
            } catch (Exception e) {
                log.error("执行SQL异常：" + sql, e);
            }
        }
        for (String sql : modifyDropSqls) {
            try {
                getJdbcTemplate().execute(sql);
            } catch (Exception e) {
                log.error("执行SQL异常：" + sql, e);
            }
        }
        for (String sql : modifyCreateSqls) {
            try {
                getJdbcTemplate().execute(sql);
            } catch (Exception e) {
                log.error("执行SQL异常：" + sql, e);
            }
        }
    }

    // 生成新增索引的sql
    private static List<String> generateAddIndex(String schemaName, String tableName, List<StatisticsDTO> statisticsDTOS) {
        // "ALTER TABLE %s.%s ADD %s INDEX %s (%s)";

        return statisticsDTOS.stream()
                .map(dto -> String.format(ADD_INDEX,
                        schemaName,
                        tableName,
                        indexTypeSet(dto),
                        dto.getIndexName().equals("PRIMARY") ? "" : dto.getIndexName(),
                        // dto.getColumns().stream().collect(Collectors.joining(","))
                        Joiner.on(",").join(dto.getColumns())
                ))
                .collect(Collectors.toList());

    }

    // 删除索引
    private static List<String> generateDropIndex(String schemaName, String tableName, List<StatisticsDTO> statisticsDTOS) {
        return statisticsDTOS.stream()
                .map(statisticsDTO -> String.format(DROP_INDEX,
                        schemaName,
                        tableName,
                        statisticsDTO.getIndexName()))
                .collect(Collectors.toList());

    }

    // diff 索引的差异
    private static Map<Boolean, List<StatisticsDTO>> diffStatistics(List<StatisticsDO> srcStatisticDos, List<StatisticsDO> dstStatisticDos) {
        // 将do 转成dto
        List<StatisticsDTO> srcDtos = fromStatisticsDOToDTO(srcStatisticDos);
        List<StatisticsDTO> dstDtos = fromStatisticsDOToDTO(dstStatisticDos);
        // diff，diffStatisticsDTOs 是包含新增与修改的，所有的DTO实例
        Set<StatisticsDTO> diffStatisticsDTOs = Sets.difference(new HashSet<>(srcDtos), new HashSet<>(dstDtos)).immutableCopy();
        // 区分哪些索引是新增的，哪些索引是变动的
        // 将src 源集合中的，IndexName所有索引名称，组成一个集合
        Set<String> srcNames = srcDtos.stream().map(StatisticsDTO::getIndexName).collect(Collectors.toSet());
        // 将dst 源集合中的，IndexName所有索引名称，组成一个集合
        Set<String> dstNames = dstDtos.stream().map(StatisticsDTO::getIndexName).collect(Collectors.toSet());
        // 在diffNames  中的实例，都是添加的。
        Set<String> diffNames = srcNames.stream().filter(i -> !dstNames.contains(i)).collect(Collectors.toSet());

        // Map，为true则是所有新增的。为false，是所有修改的。
        return diffStatisticsDTOs.stream()
                // partitioning 分区。只能分两个区，true与false
                .collect(Collectors.partitioningBy(statisticsDTO -> diffNames.contains(statisticsDTO.getIndexName())));


    }

    // 将StatisticsDo 转成 StatisticsDTO
    private static List<StatisticsDTO> fromStatisticsDOToDTO(List<StatisticsDO> statisticDos) {
        // 按照索引的名称进行分组 根据IndexName 对List<StatisticsDo> 分组。因为联合索引的原因。
        // 联合索引，索引名称一样，但是对应的列和SeqInIndex的值不一样
        Map<String, List<StatisticsDO>> dos = statisticDos.stream().collect(Collectors.groupingBy(StatisticsDO::getIndexName));

        return dos.values().stream()
                .map(statisticsDOS -> {
                    //
                    StatisticsDO sdo = statisticsDOS.get(0);
                    List<String> columns = statisticsDOS.stream()
                            .sorted(Comparator.comparingInt(StatisticsDO::getSeqInIndex))
                            .map(StatisticsDO::getColumnName).collect(Collectors.toList());
                    return StatisticsDTO.builder()
                            .indexName(sdo.getIndexName())
                            .nonUnique(sdo.getNonUnique())
                            .indexType(sdo.getIndexType())
                            .columns(columns)
                            .build();
                })
                .collect(Collectors.toList());

    }
}
