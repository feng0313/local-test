package com.chunfeng.local.mapper;

import com.chunfeng.local.model.DynamicDataRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class JdbcDatabaseAccess {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, List<DynamicDataRow>> getAllFilteredTablesData(String sinceTime, String endTime,
                                                                      List<String> tableNames) {
        log.info("===>开始汇总数据");
        String timeColumn = "create_time";
        // 缓存所有表的列信息
        Map<String, List<String>> tableColumnsCache = new HashMap<>();
        for (String tableName : tableNames) {
            tableColumnsCache.put(tableName, getTableColumns(tableName));
        }
        Map<String, List<DynamicDataRow>> allTablesData = new HashMap<>();
        int totalProcessedRecords = 0;
        for (String tableName : tableNames) {
            log.info("开始处理表: {}", tableName);
            List<DynamicDataRow> tableData = fetchTableDataSinceTime(tableName, timeColumn, sinceTime, endTime,
                    tableColumnsCache.get(tableName));
            if (tableData != null && !tableData.isEmpty()) {
                allTablesData.put(tableName, tableData);
                totalProcessedRecords += tableData.size();
            } else {
                log.info("表 {} 没有符合条件的数据", tableName);
            }
        }
        log.info("总计处理 {} 张表，操作了 {} 条数据", tableNames.size(), totalProcessedRecords);
        return allTablesData;
    }

    private List<DynamicDataRow> fetchTableDataSinceTime(String tableName, String timeColumn, String sinceTime,
                                                         String endTime,
                                                         List<String> columns) {
        // 添加反引号以确保兼容表名和列名中的特殊字符
        String selectColumns = columns.stream().map(column -> "`" + column + "`").collect(Collectors.joining(","));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime time1 = LocalDateTime.parse(sinceTime, formatter);
        LocalDateTime time2 = LocalDateTime.parse(endTime, formatter);
        // 直接在fetchTableDataSinceTime方法内部构建完整的SQL查询字符串，包含sinceTime
        String sql = String.format("SELECT %s, `%s` AS `_time_` FROM `%s` WHERE `%s` between '%s' and '%s'",
                selectColumns, timeColumn, tableName, timeColumn,
                time1.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                time2.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                DynamicDataRow row = new DynamicDataRow(tableName);
                for (int i = 0; i < columns.size(); i++) {
                    row.addField(columns.get(i), rs.getObject(i + 1)); // 注意：索引从1开始，因为第一个参数是sinceTime
                }
                return row;
            });
        } catch (Exception e) {
            log.error("表 {} 数据读取失败: {}", tableName, e.getMessage(), e);
            // 返回空列表而非null，以避免后续空指针异常
            return Collections.emptyList();
        }
    }

    private List<String> getTableColumns(String tableName) {
        String columnSql = "SELECT column_name FROM information_schema.columns WHERE table_schema = 'huayi-iot-cloud' AND table_name = ?";
        return jdbcTemplate.queryForList(columnSql, String.class, tableName);
    }
}