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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author 13994
 */
@Slf4j
@Component
public class DynamicDataWriter {

    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/huayi-iot-cloud?useUnicode=true" +
            "&characterEncoding=utf-8&useSSL=false";
    private static final String USER = "root";
    private static final String PASS = "Turing12345!";
    private static final int BATCH_SIZE = 200; // 批量处理的大小

    /**
     * 将数据写入数据库
     *
     * @param allDataMap 所有数据
     */
    public static void writeDataToDatabase(@NotNull Map<String, List<DynamicDataRow>> allDataMap) {
        log.info("===>开始写入数据，请耐心等待");
        log.info("传入数据：{}",allDataMap);
        if (!ObjectUtils.isEmpty(allDataMap)) {
            int totalInsertedRows = 0;
            int totalTablesProcessed = 0;
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                // 关闭自动提交
                conn.setAutoCommit(false);
                for (Map.Entry<String, List<DynamicDataRow>> entry : allDataMap.entrySet()) {
                    // 处理每个表前，计数器加一
                    totalTablesProcessed++;
                    String tableName = entry.getKey();
                    List<DynamicDataRow> dataRows = entry.getValue();
                    String sql = buildInsertSql(tableName, dataRows.get(0).getDataFields().keySet());
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        // 当前批次的记录数计数器
                        int counter = 0;
                        for (DynamicDataRow dataRow : dataRows) {
                            setPreparedStatementParams(pstmt, new ArrayList<>(dataRow.getDataFields().values()));
                            pstmt.addBatch();
                            counter++;
                            if (counter % BATCH_SIZE == 0 || dataRow.equals(dataRows.get(dataRows.size() - 1))) {
                                pstmt.executeBatch();
                                // 根据实际执行的批次大小累加
                                totalInsertedRows += counter;
                                // 重置计数器
                                counter = 0;
                            }
                        }
                    } catch (SQLException e) {
                        log.error("执行批量插入时发生错误并已自动回滚：{}", e.getMessage());
                        conn.rollback();
                        throw e;
                    }
                }
                // 所有数据处理完成后，提交事务
                conn.commit();
            } catch (SQLException e) {
                log.error("数据库访问异常,已自动回滚：{}", e.getMessage());
            } finally {
                // 输出统计信息
                log.info("写入完成。共插入 {} 条记录，操作 {} 个表。", totalInsertedRows, totalTablesProcessed);
            }
        }
        log.info("===>传入数据为空，无任何操作。");
    }

    private static String buildInsertSql(String tableName, Set<String> columnNames) {
        StringBuilder columns = new StringBuilder("(");
        StringBuilder values = new StringBuilder("(");
        for (String columnName : columnNames) {
            if (columns.length() > 1) {
                columns.append(", ");
                values.append(", ");
            }
            columns.append(columnName);
            values.append("?");
        }
        columns.append(")");
        values.append(")");
        return "INSERT INTO " + tableName + " " + columns + " VALUES " + values;
    }

    private static void setPreparedStatementParams(PreparedStatement pstmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }
    }
}