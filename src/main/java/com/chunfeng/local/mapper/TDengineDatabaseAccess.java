package com.chunfeng.local.mapper;

import com.chunfeng.local.model.DynamicDataRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@Slf4j
public class TDengineDatabaseAccess {

    private static final String DB_URL = "jdbc:TAOS-RS://10.210.254.39:6041?user=root&password=taosdata";

    public Map<String, List<DynamicDataRow>> getAllTablesDataSinceTimeUntil(String database, String sinceTime, String untilTime) {
        if (untilTime == null || untilTime.isEmpty()) {
            untilTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        String timeColumn = "ts";
        Map<String, List<DynamicDataRow>> allTablesData = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            executeUseDatabase(conn, database);
            List<String> allTableNames = getTableNames(conn);
            log.info("数据库中总共有 {} 张表", allTableNames.size());

            for (String tableName : allTableNames) {
                log.info("开始处理表: {}", tableName);
                List<DynamicDataRow> tableData = fetchTableDataSinceTimeUntil(conn, database, tableName, timeColumn, sinceTime, untilTime);
                if (tableData != null && !tableData.isEmpty()) {
                    allTablesData.put(tableName, tableData);
                } else {
                    log.info("表 {} 没有符合条件的数据", tableName);
                }
            }
            log.info("总计处理 {} 张表", allTableNames.size());
        } catch (SQLException e) {
            log.error("连接数据库失败", e);
            throw new IllegalStateException("无法连接数据库", e);
        }

        return allTablesData;
    }

    private void executeUseDatabase(Connection conn, String dbName) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("use " + dbName);
        }
    }

    private List<String> getTableNames(Connection conn) throws SQLException {
        List<String> allTableNames = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
            while (rs.next()) {
                allTableNames.add(rs.getString(1));
            }
        }
        return allTableNames;
    }

    private List<DynamicDataRow> fetchTableDataSinceTimeUntil(Connection conn, String database, String tableName, String timeColumn, String sinceTime, String untilTime) throws SQLException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startTime = LocalDateTime.parse(sinceTime, formatter);
        LocalDateTime endTime = LocalDateTime.parse(untilTime, formatter);
        String sql = String.format("SELECT * FROM %s.`%s` WHERE %s >= '%s' AND %s <= '%s'",
                database, tableName, timeColumn, startTime.format(formatter), timeColumn, endTime.format(formatter));
        List<DynamicDataRow> tableData = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                DynamicDataRow row = new DynamicDataRow(tableName);

                // 遍历所有列，处理TIMESTAMP类型的字段
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    int columnType = metaData.getColumnType(i);

                    if (columnType == Types.TIMESTAMP) {
                        LocalDateTime timestamp = rs.getTimestamp(i).toLocalDateTime();
                        row.addField(columnName, timestamp.format(formatter));
                    } else {
                        Object value = rs.getObject(i);
                        row.addField(columnName, value);
                    }
                }

                tableData.add(row);
            }
        }
        return tableData;
    }

}