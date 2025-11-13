package com.orderplatform.query.config;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaStreamsConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.streams.application-id}")
    private String applicationId;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${spring.kafka.streams.properties.processing.guarantee}")
    private String processingGuarantee;

    @Value("${spring.kafka.streams.properties.num.stream.threads}")
    private int numStreamThreads;

    @Value("${spring.kafka.streams.properties.commit.interval.ms}")
    private int commitIntervalMs;

    @Value("${spring.kafka.streams.properties.state.dir}")
    private String stateDir;

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, processingGuarantee);
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, numStreamThreads);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, commitIntervalMs);
        props.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        
        // Optimize for exactly-once semantics
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 3);
        props.put(StreamsConfig.producerPrefix(StreamsConfig.RETRIES_CONFIG), Integer.MAX_VALUE);
        props.put(StreamsConfig.producerPrefix("max.in.flight.requests.per.connection"), 5);
        props.put(StreamsConfig.producerPrefix("enable.idempotence"), true);
        
        return new KafkaStreamsConfiguration(props);
    }
}
