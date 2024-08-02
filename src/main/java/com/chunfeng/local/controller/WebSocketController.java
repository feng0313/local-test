package com.chunfeng.local.controller;

import com.chunfeng.local.common.config.MyStompSessionHandler;
import com.chunfeng.local.mapper.JdbcDatabaseAccess;
import com.chunfeng.local.mapper.TDengineDatabaseAccess;
import com.chunfeng.local.model.DynamicDataRow;
import com.chunfeng.local.model.QueryDataRequest;
import com.chunfeng.local.utils.HttpClientUtil;
import com.chunfeng.local.utils.MyWebSocketClient;
import com.chunfeng.local.utils.WebSocketDataReceiver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
public class WebSocketController {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SimpMessagingTemplate template;
    @Autowired
    private JdbcDatabaseAccess jdbcDatabaseAccess;
    @Autowired
    private MyWebSocketClient myWebSocketClient;
    @Autowired
    private TDengineDatabaseAccess tDengineDatabaseAccess;
    @Autowired
    private WebSocketDataReceiver webSocketDataReceiver;

    public void queryAllDataFromWebSocket(@Payload String jsonPayload) {
        ObjectMapper objectMapper = new ObjectMapper();
        QueryDataRequest request = null;
        try {
            request = objectMapper.readValue(jsonPayload, QueryDataRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        log.info("===>开始查询数据");
        Map<String, List<DynamicDataRow>> allData;
        if (request.getUrl().contains("1234567890") || request.getUrl().contains("test")) {
            allData = tDengineDatabaseAccess.getAllTablesDataSinceTimeUntil(request.getUrl(),
                    request.getStartTime(), request.getEndTime());
        } else {
            allData = jdbcDatabaseAccess.getAllFilteredTablesData(request.getStartTime(), request.getEndTime(),
                    request.getTableList());
        }
        log.info("===>数据查询完毕---开始传输数据");
        Map<String, Integer> tableDataCounts = new HashMap<>();
        for (Map.Entry<String, List<DynamicDataRow>> entry : allData.entrySet()) {
            tableDataCounts.put(entry.getKey(), entry.getValue().size());
        }
        template.convertAndSend("/topic/data_summary", tableDataCounts);
        log.info("===>已发送数据条数汇总");
        // 设置每批次的数据条数
        int batchSize = 1;
        String finalName = request.getUrl();
        allData.forEach((tableName, tableData) -> {
            log.info("{}表的条数：{}", tableName, tableData.size());
            int dataSize = tableData.size();
            int chunks = (dataSize + batchSize - 1) / batchSize; // 计算需要的批次数量，向上取整
            for (int i = 0; i < chunks; i++) {
                int start = i * batchSize;
                int end = Math.min(start + batchSize, dataSize);
                List<DynamicDataRow> batchData = tableData.subList(start, end);
                // 分批次发送数据，但不改变topic中的表名部分，只发送批次序号作为附加信息
                Map<String, Object> headers = new HashMap<>();
                headers.put("batch-index", String.valueOf(i)); // 添加批次索引作为header信息
                headers.put("name", finalName);
                template.convertAndSend("/topic/data/" + tableName, batchData, headers);
            }
        });
        int totalDataRowCount = 0;
        for (List<DynamicDataRow> list : allData.values()) {
            totalDataRowCount += list.size();
        }
        log.info("===>数据传输完毕。共{}张表，{}条数据。", allData.size(), totalDataRowCount);
    }

    @PostMapping("/startws")
    public ResponseEntity<String> startWebSocketSession(@RequestBody QueryDataRequest queryDataRequest) {
        try {
            myWebSocketClient.closeConnection();
            String url = "ws://huayi.ys7.com/source/websocket";
//            String url = "ws://localhost:48001/websocket";
//            webSocketDataReceiver.connect(url, queryDataRequest);
            StompSessionHandler handler = new MyStompSessionHandler(queryDataRequest);
            myWebSocketClient.initConnection(url, handler);
            return ResponseEntity.ok("WebSocket会话已启动。");
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.ok("启动WebSocket会话时发生错误：" + e.getMessage());
        }
    }

//    @Scheduled(cron = "*/30 * * * * ?")
    public void mySheduledTask1() {
        QueryDataRequest queryDataRequest = new QueryDataRequest();
        queryDataRequest.setUrl("property_1234567890");
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMinutes(5);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        queryDataRequest.setStartTime(startTime.format(formatter));
        queryDataRequest.setEndTime(endTime.format(formatter));
        log.info("定时任务执行property同步，开始时间：{}，结束时间{}", queryDataRequest.getStartTime(), queryDataRequest.getEndTime());
        qidong(queryDataRequest);
    }


//    @Scheduled(cron = "*/30 * * * * ?")
    public void myScheduledTask2() {
        QueryDataRequest queryDataRequest = new QueryDataRequest();
        queryDataRequest.setUrl("event_1234567890");
        // 计算35秒前的时间
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMinutes(5);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        queryDataRequest.setStartTime(startTime.format(formatter));
        queryDataRequest.setEndTime(endTime.format(formatter));
        log.info("定时任务执行event同步，开始时间：{}，结束时间{}", queryDataRequest.getStartTime(), queryDataRequest.getEndTime());
        qidong(queryDataRequest);
    }


//    @Scheduled(cron = "*/30 * * * * ?")
    public void myScheduledTask() {
        log.info("定时任务执行huayi-iot-cloud同步");
        QueryDataRequest queryDataRequest = new QueryDataRequest();
        List<String> tableNames = Arrays.asList(
                "ezviz_item_device_history",
                "ezviz_user_device",
                "iot_group",
                "iot_platform",
                "item",
                "item_category",
                "item_device",
                "item_metadata",
                "item_provider",
                "item_tag",
                "huayi_sleep_staging_main_sleep",
                "huayi_sleep_staging_raw_result",
                "huayi_sleep_staging_result",
                "huayi_sleep_staging_sleep_cycle",
                "huayi_sleep_staging_status",
                "huayi_sleep_staging_status2",
                "statistic_breath_daily",
                "statistic_heart_breath_10minute",
                "statistic_heart_daily",
                "statistic_sleep_daily",
                "statistic_sleep_stage"
        );
        queryDataRequest.setTableList(tableNames);
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMinutes(5);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        queryDataRequest.setStartTime(startTime.format(formatter));
        queryDataRequest.setEndTime(endTime.format(formatter));
        queryDataRequest.setUrl("huayi-iot-cloud");
        qidong(queryDataRequest);
    }


    @PostMapping("/get")
    public void queryData(@RequestBody QueryDataRequest request, HttpServletResponse response) throws IOException {
        log.info("===>开始查询数据");
        Map<String, List<DynamicDataRow>> allData;
        if (request.getUrl().contains("1234567890") || request.getUrl().contains("test")) {
            allData = tDengineDatabaseAccess.getAllTablesDataSinceTimeUntil(request.getUrl(),
                    request.getStartTime(), request.getEndTime());
        } else {
            allData = jdbcDatabaseAccess.getAllFilteredTablesData(request.getStartTime(), request.getEndTime(),
                    request.getTableList());
        }
        // 将allData转换为JSON字符串
        String jsonResponse = objectMapper.writeValueAsString(allData);
        // 设置响应头和内容类型
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // 直接写入响应输出流
        response.getWriter().write(jsonResponse);
    }

    @PostMapping("/qidong")
    public void qidong(@RequestBody QueryDataRequest queryDataRequest) {
        new HttpClientUtil().test(queryDataRequest);
    }
}