package com.chunfeng.local;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

@SpringBootApplication
@EnableWebSocketMessageBroker
public class LocalTestApplication {

    public static void main(String[] args) {
        SpringApplication run = new SpringApplication(LocalTestApplication.class);
        run.run(args);
    }

}