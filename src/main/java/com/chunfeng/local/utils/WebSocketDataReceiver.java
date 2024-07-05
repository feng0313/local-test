package com.chunfeng.local.utils;

import com.chunfeng.local.model.DynamicDataRow;
import com.chunfeng.local.model.QueryDataRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
public class WebSocketDataReceiver {

    private final WebSocketStompClient stompClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<DynamicDataRow>> dataMap = new HashMap<>();
    private StompSession session;

    public WebSocketDataReceiver(WebSocketStompClient stompClient) {
        this.stompClient = stompClient;
    }

    public void connect(String url, QueryDataRequest queryDataRequest) throws ExecutionException, InterruptedException {
        try {
            session = stompClient.connect(url, new StompSessionHandlerAdapter() {
            }).get();
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonDataRequest = objectMapper.writeValueAsString(queryDataRequest);
            session.send("/app/queryAllData", jsonDataRequest);
            log.info("已发送查询所有数据的请求，携带参数：{}", jsonDataRequest);
        } catch (JsonProcessingException e) {
            log.error("序列化查询数据请求为JSON时发生错误", e);
        }
        session.subscribe("/topic/data_summary", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                Map<String, Integer> summary = (Map<String, Integer>) payload;
                System.out.println("Data Summary Received: " + summary);
                // 可以在这里初始化或更新数据结构准备接收详细数据
            }
        });

        session.subscribe("/topic/data/{tableName}", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return List.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                List<DynamicDataRow> batchData = (List<DynamicDataRow>) payload;
                String tableName = headers.getDestination().split("/")[3]; // 假设topic格式为"/topic/data/{tableName}"
                collectData(tableName, batchData);
            }
        });
    }

    private void collectData(String tableName, List<DynamicDataRow> batchData) {
        dataMap.computeIfAbsent(tableName, k -> new ArrayList<>()).addAll(batchData);
        // 根据需要，可以在这里检查是否所有数据都已接收完成
    }

    // 其他方法，如disconnect等...
}