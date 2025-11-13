package com.orderplatform.query.api;

import com.orderplatform.query.readmodel.OrderReadModel;
import com.orderplatform.query.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderQueryController {

    private final OrderQueryService orderQueryService;

    @GetMapping
    public ResponseEntity<Page<OrderReadModel>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        log.debug("Query orders - status: {}, customerId: {}, fromDate: {}, toDate: {}, page: {}, size: {}",
            status, customerId, fromDate, toDate, page, size);

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<OrderReadModel> orders = orderQueryService.findOrders(
            status, customerId, fromDate, toDate, pageable
        );

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<OrderReadModel>> searchOrders(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.debug("Search orders - query: {}, page: {}, size: {}", q, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<OrderReadModel> orders = orderQueryService.searchOrders(q, pageable);

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderReadModel> getOrderById(@PathVariable String id) {
        log.debug("Get order by ID: {}", id);

        Optional<OrderReadModel> order = orderQueryService.findById(id);

        return order
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
