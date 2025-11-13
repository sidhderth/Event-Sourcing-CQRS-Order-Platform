package com.orderplatform.query.infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexManager {

    private final ElasticsearchClient elasticsearchClient;
    private static final String ORDERS_INDEX = "orders_v1";

    @PostConstruct
    public void initializeIndices() {
        try {
            if (!indexExists(ORDERS_INDEX)) {
                createOrdersIndex();
                log.info("Created Elasticsearch index: {}", ORDERS_INDEX);
            } else {
                log.info("Elasticsearch index already exists: {}", ORDERS_INDEX);
            }
        } catch (IOException e) {
            log.error("Failed to initialize Elasticsearch indices", e);
            throw new RuntimeException("Failed to initialize Elasticsearch indices", e);
        }
    }

    private boolean indexExists(String indexName) throws IOException {
        return elasticsearchClient.indices()
            .exists(ExistsRequest.of(e -> e.index(indexName)))
            .value();
    }

    private void createOrdersIndex() throws IOException {
        TypeMapping mapping = TypeMapping.of(tm -> tm
            .properties("orderId", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
            .properties("customerId", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
            .properties("status", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
            .properties("items", Property.of(p -> p.nested(NestedProperty.of(n -> n
                .properties("sku", Property.of(ip -> ip.keyword(KeywordProperty.of(k -> k))))
                .properties("productName", Property.of(ip -> ip.text(TextProperty.of(t -> t
                    .fields("keyword", Property.of(fp -> fp.keyword(KeywordProperty.of(k -> k))))
                ))))
                .properties("quantity", Property.of(ip -> ip.integer(IntegerNumberProperty.of(i -> i))))
                .properties("unitPrice", Property.of(ip -> ip.scaledFloat(ScaledFloatNumberProperty.of(sf -> sf
                    .scalingFactor(100.0)
                ))))
                .properties("lineTotal", Property.of(ip -> ip.scaledFloat(ScaledFloatNumberProperty.of(sf -> sf
                    .scalingFactor(100.0)
                ))))
            ))))
            .properties("totalAmount", Property.of(p -> p.scaledFloat(ScaledFloatNumberProperty.of(sf -> sf
                .scalingFactor(100.0)
            ))))
            .properties("currency", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
            .properties("createdAt", Property.of(p -> p.date(DateProperty.of(d -> d))))
            .properties("updatedAt", Property.of(p -> p.date(DateProperty.of(d -> d))))
            .properties("version", Property.of(p -> p.long_(LongNumberProperty.of(l -> l))))
            .properties("approvedBy", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
            .properties("rejectionReason", Property.of(p -> p.text(TextProperty.of(t -> t))))
            .properties("trackingNumber", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
            .properties("carrier", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))))
        );

        IndexSettings settings = IndexSettings.of(s -> s
            .numberOfShards("3")
            .numberOfReplicas("1")
            .refreshInterval(t -> t.time("1s"))
        );

        CreateIndexRequest request = CreateIndexRequest.of(c -> c
            .index(ORDERS_INDEX)
            .mappings(mapping)
            .settings(settings)
        );

        elasticsearchClient.indices().create(request);
    }

    public String getOrdersIndexName() {
        return ORDERS_INDEX;
    }
}
