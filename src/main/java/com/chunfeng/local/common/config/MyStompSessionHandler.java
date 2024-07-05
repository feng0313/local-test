package com.chunfeng.local.common.config;

import com.chunfeng.local.mapper.DynamicDataWriter;
import com.chunfeng.local.mapper.TdengineWritter;
import com.chunfeng.local.model.DynamicDataRow;
import com.chunfeng.local.model.QueryDataRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 13994
 */
@Slf4j
@EnableAsync
public class MyStompSessionHandler extends StompSessionHandlerAdapter {

    private final Map<String, List<DynamicDataRow>> allDataMap = new HashMap<>();
    private final Map<String, Integer> expectedDataCounts = new ConcurrentHashMap<>();
    private final Set<String> completedTables = ConcurrentHashMap.newKeySet();

    private final QueryDataRequest dataRequest;
    private final Map<String, List<DynamicDataRow>> dataMap = new HashMap<>();
    public MyStompSessionHandler(QueryDataRequest queryDataRequest) {
        this.dataRequest = queryDataRequest;
    }

    @SneakyThrows
    @Override
    public void afterConnected(StompSession session, @NotNull StompHeaders connectedHeaders) {
        log.info("已连接到服务器，连接头信息：{}", connectedHeaders);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonDataRequest = objectMapper.writeValueAsString(dataRequest);
            session.send("/app/queryAllData", jsonDataRequest);
            log.info("已发送查询所有数据的请求，携带参数：{}", jsonDataRequest);
        } catch (JsonProcessingException e) {
            log.error("序列化查询数据请求为JSON时发生错误", e);
        }
        session.subscribe("/topic/data_summary", new StompFrameHandler() {
            @NotNull
            @Override
            public Type getPayloadType(@NotNull StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(@NotNull StompHeaders headers, Object payload) {
                Map<String, Integer> tableDataCounts = (Map<String, Integer>) payload;
                expectedDataCounts.putAll(tableDataCounts);
                log.info("数据条数汇总信息已更新且实际计数已初始化: {}", expectedDataCounts);
            }
        });
        session.subscribe("/topic/data/{tableName}", new StompFrameHandler() {

            @NotNull
            @Override
            public Type getPayloadType(@NotNull StompHeaders headers) {
                return List.class;
            }

            @Override
            @SneakyThrows
            public void handleFrame(@NotNull StompHeaders headers, Object payload) {
                List<DynamicDataRow> batchData = (List<DynamicDataRow>) payload;
                String tableName = headers.getDestination().split("/")[3]; // 假设topic格式为"/topic/data/{tableName}"
                collectData(tableName, batchData);
//                String tableName = Objects.requireNonNull(headers.getDestination()).replaceFirst("/topic/data/", "");
//                List<DynamicDataRow> dataBatch = new ArrayList<>();
//                for (Object o : (List) payload) {
//                    DynamicDataRow dataRow = new DynamicDataRow(tableName);
//                    Map<String, Object> map = (Map<String, Object>) o;
//                    dataRow.setDataFields((Map<String, Object>) map.get("dataFields"));
//                    dataBatch.add(dataRow);
//                }
////                log.info("接收到消息，头部信息为：{}", headers);
//                allDataMap.computeIfAbsent(tableName, k -> new ArrayList<>()).addAll(dataBatch);
//                processAllData(tableName, dataRequest.getUrl());
            }
        });
        log.info("已发送查询所有数据的请求，时间戳：{}", dataRequest.getStartTime());
    }

    private void collectData(String tableName, List<DynamicDataRow> batchData) {
        log.info("接收到消息，头部信息为：{}", tableName);
        log.info("接收到消息，头部信息为：{}", batchData);
//        dataMap.computeIfAbsent(tableName, k -> new ArrayList<>()).addAll(batchData);
        // 根据需要，可以在这里检查是否所有数据都已接收完成
    }

    private void processAllData(String tableName, String name) {
        List<DynamicDataRow> allData = allDataMap.get(tableName);
        Integer expectedCount = expectedDataCounts.get(tableName);
        int actualCount = allData != null ? allData.size() : 0;

        if (expectedCount != null) {
            if (actualCount == expectedCount) {
                completedTables.add(tableName);
                // 检查所有表是否都已完成接收
                if (completedTables.containsAll(expectedDataCounts.keySet())) {
                    log.info("所有表的数据接收完成，开始最终处理...");
                    if (name.contains("1234567890") || name.contains("test")) {
//                        new TdengineWritter().writeDataToDatabase(allDataMap, "db");
                        new TdengineWritter().writeDataToDatabase(allDataMap, name);
                    } else {
                        DynamicDataWriter.writeDataToDatabase(allDataMap);
                    }
                }
            }
        } else {
            log.warn("表 {} 无预期数据条数设置，实际接收数据条数：{}", tableName, actualCount);
        }
    }

    @Override
    public void handleException(@NotNull StompSession session, StompCommand command, @NotNull StompHeaders headers, @NotNull byte[] payload, @NotNull Throwable exception) {
        exception.printStackTrace();
        String jsonString = new String(payload, StandardCharsets.UTF_8);
        log.error("客户端错误: 异常 {}, command {}, payload {}, headers {}", exception, command, jsonString, headers);
    }

    @Override
    public void handleTransportError(@NotNull StompSession session, Throwable exception) {
        exception.printStackTrace();
        log.error("客户端传输错误：错误 {}", exception.getMessage());
    }
}