package com.orderplatform.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableKafkaStreams
public class OrderQueryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderQueryServiceApplication.class, args);
    }
}
