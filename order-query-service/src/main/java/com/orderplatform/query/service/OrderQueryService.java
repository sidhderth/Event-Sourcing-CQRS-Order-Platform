package com.orderplatform.query.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.orderplatform.query.infrastructure.elasticsearch.ElasticsearchIndexManager;
import com.orderplatform.query.readmodel.OrderReadModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchIndexManager indexManager;

    // Whitelist of allowed filter fields for security
    private static final List<String> ALLOWED_FILTER_FIELDS = List.of(
        "status", "customerId", "createdAt", "updatedAt"
    );

    public Page<OrderReadModel> findOrders(
            String status,
            String customerId,
            Instant fromDate,
            Instant toDate,
            Pageable pageable
    ) {
        try {
            Query query = buildFilterQuery(status, customerId, fromDate, toDate);

            SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(indexManager.getOrdersIndexName())
                .query(query)
                .from((int) pageable.getOffset())
                .size(pageable.getPageSize())
                .sort(buildSortOptions(pageable))
            );

            SearchResponse<OrderReadModel> response = elasticsearchClient.search(
                searchRequest,
                OrderReadModel.class
            );

            List<OrderReadModel> orders = response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());

            long total = response.hits().total() != null ? response.hits().total().value() : 0;

            return new PageImpl<>(orders, pageable, total);

        } catch (IOException e) {
            log.error("Error querying orders", e);
            throw new RuntimeException("Failed to query orders", e);
        }
    }

    public Page<OrderReadModel> searchOrders(String searchQuery, Pageable pageable) {
        try {
            Query query = buildFullTextSearchQuery(searchQuery);

            SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(indexManager.getOrdersIndexName())
                .query(query)
                .from((int) pageable.getOffset())
                .size(pageable.getPageSize())
            );

            SearchResponse<OrderReadModel> response = elasticsearchClient.search(
                searchRequest,
                OrderReadModel.class
            );

            List<OrderReadModel> orders = response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());

            long total = response.hits().total() != null ? response.hits().total().value() : 0;

            return new PageImpl<>(orders, pageable, total);

        } catch (IOException e) {
            log.error("Error searching orders", e);
            throw new RuntimeException("Failed to search orders", e);
        }
    }

    public Optional<OrderReadModel> findById(String orderId) {
        try {
            GetRequest getRequest = GetRequest.of(g -> g
                .index(indexManager.getOrdersIndexName())
                .id(orderId)
            );

            GetResponse<OrderReadModel> response = elasticsearchClient.get(
                getRequest,
                OrderReadModel.class
            );

            if (response.found()) {
                return Optional.ofNullable(response.source());
            }

            return Optional.empty();

        } catch (IOException e) {
            log.error("Error getting order by ID: {}", orderId, e);
            throw new RuntimeException("Failed to get order", e);
        }
    }

    private Query buildFilterQuery(String status, String customerId, Instant fromDate, Instant toDate) {
        List<Query> mustQueries = new ArrayList<>();

        // Filter by status
        if (status != null && !status.isBlank()) {
            mustQueries.add(Query.of(q -> q
                .term(t -> t
                    .field("status")
                    .value(FieldValue.of(status))
                )
            ));
        }

        // Filter by customerId
        if (customerId != null && !customerId.isBlank()) {
            mustQueries.add(Query.of(q -> q
                .term(t -> t
                    .field("customerId")
                    .value(FieldValue.of(customerId))
                )
            ));
        }

        // Filter by date range
        if (fromDate != null || toDate != null) {
            RangeQuery.Builder rangeBuilder = new RangeQuery.Builder()
                .field("createdAt");

            if (fromDate != null) {
                rangeBuilder.gte(co.elastic.clients.json.JsonData.of(fromDate.toEpochMilli()));
            }
            if (toDate != null) {
                rangeBuilder.lte(co.elastic.clients.json.JsonData.of(toDate.toEpochMilli()));
            }

            mustQueries.add(Query.of(q -> q.range(rangeBuilder.build())));
        }

        // If no filters, return match_all query
        if (mustQueries.isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        // Combine all filters with bool query
        return Query.of(q -> q
            .bool(b -> b.must(mustQueries))
        );
    }

    private Query buildFullTextSearchQuery(String searchQuery) {
        // Sanitize input to prevent injection
        String sanitizedQuery = sanitizeSearchQuery(searchQuery);

        // Search across nested items (sku and productName)
        return Query.of(q -> q
            .bool(b -> b
                .should(s -> s
                    .nested(n -> n
                        .path("items")
                        .query(nq -> nq
                            .multiMatch(mm -> mm
                                .query(sanitizedQuery)
                                .fields("items.sku", "items.productName")
                                .type(TextQueryType.BestFields)
                            )
                        )
                    )
                )
                .should(s -> s
                    .multiMatch(mm -> mm
                        .query(sanitizedQuery)
                        .fields("orderId", "customerId")
                        .type(TextQueryType.BestFields)
                    )
                )
                .minimumShouldMatch("1")
            )
        );
    }

    private String sanitizeSearchQuery(String query) {
        if (query == null) {
            return "";
        }
        // Remove special characters that could be used for injection
        return query.replaceAll("[<>{}\\[\\]\"'\\\\]", "").trim();
    }

    private List<co.elastic.clients.elasticsearch._types.SortOptions> buildSortOptions(Pageable pageable) {
        return pageable.getSort().stream()
            .map(order -> co.elastic.clients.elasticsearch._types.SortOptions.of(so -> so
                .field(f -> f
                    .field(order.getProperty())
                    .order(order.isAscending() ? SortOrder.Asc : SortOrder.Desc)
                )
            ))
            .collect(Collectors.toList());
    }
}
