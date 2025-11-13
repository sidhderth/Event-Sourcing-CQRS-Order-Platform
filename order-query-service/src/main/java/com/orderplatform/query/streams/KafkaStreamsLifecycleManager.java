package com.orderplatform.query.streams;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaStreamsLifecycleManager {

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    @Value("${maintenance.mode.enabled}")
    private boolean maintenanceModeEnabled;

    @PostConstruct
    public void init() {
        if (maintenanceModeEnabled) {
            log.warn("=".repeat(80));
            log.warn("MAINTENANCE MODE ENABLED");
            log.warn("Kafka Streams will not process events.");
            log.warn("Query endpoints will return 503 Service Unavailable.");
            log.warn("=".repeat(80));
        } else {
            log.info("Service starting in normal mode. Kafka Streams will process events.");
        }
    }

    public void pauseStreams() {
        try {
            KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
            if (kafkaStreams != null && kafkaStreams.state().isRunningOrRebalancing()) {
                log.info("Pausing Kafka Streams for maintenance mode");
                streamsBuilderFactoryBean.stop();
            }
        } catch (Exception e) {
            log.error("Error pausing Kafka Streams", e);
        }
    }

    public void resumeStreams() {
        try {
            log.info("Resuming Kafka Streams from maintenance mode");
            streamsBuilderFactoryBean.start();
        } catch (Exception e) {
            log.error("Error resuming Kafka Streams", e);
        }
    }
}
