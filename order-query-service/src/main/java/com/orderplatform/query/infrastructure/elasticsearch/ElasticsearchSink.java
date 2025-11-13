package com.orderplatform.query.infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.orderplatform.query.readmodel.OrderReadModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchSink {

    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchIndexManager indexManager;

    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2),
        retryFor = {IOException.class}
    )
    public void indexOrder(OrderReadModel order) {
        try {
            if (order == null || order.getOrderId() == null) {
                log.warn("Skipping null order or order with null ID");
                return;
            }

            IndexRequest<OrderReadModel> request = IndexRequest.of(i -> i
                .index(indexManager.getOrdersIndexName())
                .id(order.getOrderId())
                .document(order)
            );

            IndexResponse response = elasticsearchClient.index(request);

            log.debug("Indexed order {} with result: {}", 
                order.getOrderId(), 
                response.result().jsonValue()
            );

        } catch (IOException e) {
            log.error("Failed to index order: {}", order.getOrderId(), e);
            throw new RuntimeException("Failed to index order to Elasticsearch", e);
        }
    }

    public void deleteOrder(String orderId) {
        try {
            elasticsearchClient.delete(d -> d
                .index(indexManager.getOrdersIndexName())
                .id(orderId)
            );
            log.debug("Deleted order {} from Elasticsearch", orderId);
        } catch (IOException e) {
            log.error("Failed to delete order: {}", orderId, e);
            throw new RuntimeException("Failed to delete order from Elasticsearch", e);
        }
    }
}
