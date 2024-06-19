package com.chunfeng.local.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
public class MyWebSocketClient {

    private final WebSocketStompClient stompClient;
    private StompSession session;

    public MyWebSocketClient() {
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        transports.add(new RestTemplateXhrTransport());
        SockJsClient sockJsClient = new SockJsClient(transports);
        WebSocketStompClient webSocketStompClient = new WebSocketStompClient(sockJsClient);
        webSocketStompClient.setMessageConverter(new StringMessageConverter());
        sockJsClient.setMessageCodec(new Jackson2SockJsMessageCodec(new ObjectMapper()));
        this.stompClient = new WebSocketStompClient(sockJsClient);
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    /**
     * 初始化WebSocket连接。
     *
     * @param url     WebSocket服务器的URL。
     * @param handler 自定义的Stomp会话处理器。
     * @throws ExecutionException   异常抛出，当连接尝试失败时。
     * @throws InterruptedException 异常抛出，如果当前线程被中断。
     */
    public void initConnection(String url, StompSessionHandler handler) throws ExecutionException, InterruptedException {
        this.session = stompClient.connect(url, handler).get();
        log.info("WebSocket 连接已建立。");
    }

    // 可以添加其他方法来发送消息、订阅等，使用session对象

    // 示例：添加一个方法来关闭连接
    public void closeConnection() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}