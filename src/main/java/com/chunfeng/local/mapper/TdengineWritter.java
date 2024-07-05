package com.chunfeng.local.mapper;

import com.chunfeng.local.model.DynamicDataRow;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author 13994
 */
@Slf4j
@Component
public class TdengineWritter {

    private static final String DB_URL = "jdbc:TAOS-RS://10.210.254.39:6041?user=root&password=taosdata";
    private static final int MAX_SQL_SIZE_BYTES = 800 * 1024; // 假定的SQL语句最大字节数限制
    private static final int AVG_RECORD_OVERHEAD = 50; // 单条记录额外开销估计

    public void writeDataToDatabase(@NotNull Map<String, List<DynamicDataRow>> allDataMap, String dbName) {
        log.info("===>开始写入TDengine数据，请耐心等待");
        if (!ObjectUtils.isEmpty(allDataMap)) {
            int totalInsertedRows = 0;
            int totalTablesProcessed = 0;
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                conn.setAutoCommit(false);
                for (Map.Entry<String, List<DynamicDataRow>> entry : allDataMap.entrySet()) {
                    totalTablesProcessed++;
                    String tableName = entry.getKey();
                    List<DynamicDataRow> dataRows = entry.getValue();
                    executeBatchWithSizeLimit(conn, dbName, tableName, dataRows, MAX_SQL_SIZE_BYTES);
                    totalInsertedRows += dataRows.size();
                }
                conn.commit();
            } catch (SQLException e) {
                log.error("TDengine数据库访问异常,已自动回滚本次写入：{}", e.getMessage());
            } finally {
                log.info("写入TDengine完成。共插入 {} 条记录，操作 {} 个表。", totalInsertedRows, totalTablesProcessed);
            }
        } else {
            log.info("===>传入数据为空，无任何操作。");
        }
    }

    private void executeBatchWithSizeLimit(Connection conn, String dbName, String tableName, List<DynamicDataRow> dataRows, int maxSqlSizeBytes) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO ").append(dbName).append(".`").append(tableName).append("` (");
        // 假设dataFields中包含所有需要插入的列，且所有数据行具有相同的列结构
        if (!dataRows.isEmpty()) {
            DynamicDataRow sampleRow = dataRows.get(0);
            Map<String, Object> sampleFields = sampleRow.getDataFields();
            boolean isFirstColumn = true;
            for (String columnName : sampleFields.keySet()) {
                if (!isFirstColumn) {
                    sqlBuilder.append(",");
                }
                sqlBuilder.append("`").append(columnName).append("`");
                isFirstColumn = false;
            }
            sqlBuilder.append(") VALUES ");
        }
        int byteSizeEstimate = 0;
        for (DynamicDataRow dataRow : dataRows) {
            Map<String, Object> dataFields = dataRow.getDataFields();
            int recordByteSize = estimateRecordSize(dataFields);
            if (byteSizeEstimate + recordByteSize + AVG_RECORD_OVERHEAD > maxSqlSizeBytes) {
                executeSqlBatch(conn, sqlBuilder.toString());
                sqlBuilder.setLength(0);
                sqlBuilder.append("INSERT INTO ").append(dbName).append(".`").append(tableName).append("` (");

                // 重新添加列名，因为sqlBuilder被清空了
                DynamicDataRow sampleRow = dataRows.get(0);
                Map<String, Object> sampleFields = sampleRow.getDataFields();
                boolean isFirstColumn = true;
                for (String columnName : sampleFields.keySet()) {
                    if (!isFirstColumn) {
                        sqlBuilder.append(",");
                    }
                    sqlBuilder.append("`").append(columnName).append("`");
                    isFirstColumn = false;
                }
                sqlBuilder.append(") VALUES ");
                byteSizeEstimate = 0;
            }

            sqlBuilder.append("(");
            boolean isFirstValue = true;
            for (Map.Entry<String, Object> entry : dataFields.entrySet()) {
                if (!isFirstValue) {
                    sqlBuilder.append(",");
                }
                sqlBuilder.append("'").append(escapeSqlValue(entry.getValue())).append("'");
                isFirstValue = false;
            }
            sqlBuilder.append("),");
            byteSizeEstimate += recordByteSize + AVG_RECORD_OVERHEAD;
        }

        // 执行剩余的SQL
        if (sqlBuilder.length() > 0) {
            sqlBuilder.setLength(sqlBuilder.length() - 1); // 移除最后一个逗号和逗号后的空格
            executeSqlBatch(conn, sqlBuilder.toString());
        }
    }

    private void executeSqlBatch(Connection conn, String sql) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        }
    }

    private int estimateRecordSize(Map<String, Object> dataFields) {
        int size = 0;
        for (Object value : dataFields.values()) {
            if (value instanceof String) {
                size += ((String) value).length();
            } else {
                // 简单估计其他类型大小
                size += 50;
            }
        }
        return size;
    }

    private String escapeSqlValue(Object value) {
        if (value instanceof String) {
            return ((String) value).replace("'", "''");
        }
        return String.valueOf(value);
    }
}