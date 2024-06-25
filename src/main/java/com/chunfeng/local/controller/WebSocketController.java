package com.chunfeng.local.controller;

import com.chunfeng.local.common.config.MyStompSessionHandler;
import com.chunfeng.local.mapper.DynamicDataWriter;
import com.chunfeng.local.mapper.JdbcDatabaseAccess;
import com.chunfeng.local.model.DynamicDataRow;
import com.chunfeng.local.model.QueryDataRequest;
import com.chunfeng.local.model.RequestMessage;
import com.chunfeng.local.model.ResponseMessage;
import com.chunfeng.local.utils.MyWebSocketClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate template;
    @Autowired
    private JdbcDatabaseAccess jdbcDatabaseAccess;
    @Autowired
    private MyWebSocketClient myWebSocketClient;

    @MessageMapping("/queryAllData")
    @SendTo("/topic/data")
    public void queryAllDataFromWebSocket(@Payload String jsonPayload) {
        ObjectMapper objectMapper = new ObjectMapper();
        QueryDataRequest request = null;
        try {
            request = objectMapper.readValue(jsonPayload, QueryDataRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        log.info("===>开始查询数据");
        Map<String, List<DynamicDataRow>> allData =
                jdbcDatabaseAccess.getAllFilteredTablesData(request.getStartTime(), request.getTableList());
        log.info("===>数据查询完毕---开始传输数据");
        Map<String, Integer> tableDataCounts = new HashMap<>();
        for (Map.Entry<String, List<DynamicDataRow>> entry : allData.entrySet()) {
            tableDataCounts.put(entry.getKey(), entry.getValue().size());
        }
        template.convertAndSend("/topic/data_summary", tableDataCounts);
        log.info("===>已发送数据条数汇总");
        // 设置每批次的数据条数
        int batchSize = 15;
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
                template.convertAndSend("/topic/data/" + tableName, batchData, headers);
                log.info("已发送 {} 表的第 {} 批数据，范围从 {} 到 {}", tableName, (i + 1), start, end);
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
            String url = "ws://testhuayi.ys7.com/source/websocket";
//            String url = "ws://127.0.0.1:48001/websocket";
            StompSessionHandler handler = new MyStompSessionHandler(queryDataRequest);
            myWebSocketClient.initConnection(url, handler);
            return ResponseEntity.ok("WebSocket会话已启动。");
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.ok("启动WebSocket会话时发生错误：" + e.getMessage());
        }

    }

    @PostMapping("/startws1")
    public ResponseEntity<String> startWebSocketSession1(@RequestBody QueryDataRequest queryDataRequest) {
        Map<String, List<DynamicDataRow>> map =
                jdbcDatabaseAccess.getAllFilteredTablesData(queryDataRequest.getStartTime(), queryDataRequest.getTableList());
        Map<String, List<DynamicDataRow>> listMap = removeDuplicatesAndPrepareForImport(map, map);
        if (ObjectUtils.isEmpty(listMap)) {
            return ResponseEntity.ok("没有需要导入的数据。");
        }
        DynamicDataWriter.writeDataToDatabase(listMap);
        return ResponseEntity.ok("会话已启动。传输开始");
    }

    /**
     * 对比并从第一个Map中移除与第二个Map中重复的数据行。
     * 保留第一个Map中去重后的数据，准备用于入库。
     *
     * @param newDataToImport 第一个Map，待入库的外部数据
     * @param existingDataLocally 第二个Map，本地查询出的数据
     * @return 去重后的第一个Map，用于入库
     */
    public Map<String, List<DynamicDataRow>> removeDuplicatesAndPrepareForImport(
            Map<String, List<DynamicDataRow>> newDataToImport,
            Map<String, List<DynamicDataRow>> existingDataLocally) {
        if (newDataToImport == null || existingDataLocally == null) {
            log.info("输入数据Map为空，无法继续去重操作。");
            return null;
        }
        // 创建一个HashSet来存储已存在于本地数据中的数据行字段组合，用于快速查找
        Set<Map<String, Object>> seenInLocal = new HashSet<>();
        for (List<DynamicDataRow> localDataList : existingDataLocally.values()) {
            if (localDataList != null) {
                for (DynamicDataRow row : localDataList) {
                    if (row != null) {
                        seenInLocal.add(new HashMap<>(row.getDataFields()));
                    } else {
                        log.info("在本地数据中遇到空的DynamicDataRow实例，已跳过。");
                    }
                }
            } else {
                log.info("在本地数据Map中遇到空的列表，已跳过。");
            }
        }
        // 遍历待入库数据，移除与本地数据重复的项
        int duplicatesRemoved = 0;
        for (List<DynamicDataRow> importDataList : newDataToImport.values()) {
            if (importDataList != null) {
                Iterator<DynamicDataRow> iterator = importDataList.iterator();
                while (iterator.hasNext()) {
                    DynamicDataRow importRow = iterator.next();
                    if (importRow != null && seenInLocal.contains(new HashMap<>(importRow.getDataFields()))) {
                        iterator.remove(); // 移除重复项
                        duplicatesRemoved++;
                    } else if (importRow == null) {
                        log.info("在导入数据中遇到空的DynamicDataRow实例，已跳过。");
                    }
                }
            } else {
                log.info("在导入数据Map中遇到空的列表，已跳过。");
            }
        }
        log.info("成功从导入数据中移除 {} 个重复项。", duplicatesRemoved);
        Iterator<Map.Entry<String, List<DynamicDataRow>>> iterator = newDataToImport.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<DynamicDataRow>> entry = iterator.next();
            if (entry.getValue().isEmpty()) {
                iterator.remove();
                log.debug("移除空数据表: {}", entry.getKey());
            }
        }
        return newDataToImport.isEmpty() ? null : newDataToImport;
    }


    /**
     * @param requestMessage
     * @return
     * @MessageMapping 指定要接收消息的地址，类似@RequestMapping。除了注解到方法上，也可以注解到类上
     * @SendTo默认 消息将被发送到与传入消息相同的目的地
     */
//    @MessageMapping("/sendTest")
//    @SendTo("/topic/subscribeTest")
    public ResponseMessage broadcast(RequestMessage requestMessage) {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setResponseMessage("你发送的消息为:" + requestMessage.getName());
        return responseMessage;
    }

//    @SubscribeMapping("/subscribeTest")
    public ResponseMessage sub() {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setResponseMessage("感谢你订阅了我");
        return responseMessage;
    }

//    @GetMapping("/indexaaa")
    @ResponseBody
    public ResponseEntity gg() {
        template.convertAndSend("/topic/getResponse", new ResponseMessage("通过SimpMessagingTemplate进行消息广播该方式无法直接被使用，需要建立连接后访问使用,"));

        return ResponseEntity.ok("");
    }

//    @RequestMapping(value = "/index")
    public String broadcastIndex(HttpServletRequest req) {
        System.out.println(req.getRemoteHost());
        return "index";
    }
}