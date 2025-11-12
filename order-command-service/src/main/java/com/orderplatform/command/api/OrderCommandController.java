package com.orderplatform.command.api;

import com.orderplatform.command.application.OrderCommandService;
import com.orderplatform.command.application.dto.OrderResponse;
import com.orderplatform.domain.commands.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
@Slf4j
public class OrderCommandController {

    private final OrderCommandService commandService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderCommand command) {
        log.info("Received CreateOrder request for customer {}", command.customerId());
        OrderResponse response = commandService.createOrder(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{orderId}/approve")
    public ResponseEntity<OrderResponse> approveOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody ApproveOrderCommand command) {
        log.info("Received ApproveOrder request for order {}", orderId);
        
        // Ensure orderId in path matches command
        ApproveOrderCommand updatedCommand = new ApproveOrderCommand(
                orderId,
                command.approvedBy(),
                command.reason()
        );
        
        OrderResponse response = commandService.approveOrder(updatedCommand);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<OrderResponse> rejectOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody RejectOrderCommand command) {
        log.info("Received RejectOrder request for order {}", orderId);
        
        RejectOrderCommand updatedCommand = new RejectOrderCommand(
                orderId,
                command.rejectedBy(),
                command.reason()
        );
        
        OrderResponse response = commandService.rejectOrder(updatedCommand);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody CancelOrderCommand command) {
        log.info("Received CancelOrder request for order {}", orderId);
        
        CancelOrderCommand updatedCommand = new CancelOrderCommand(
                orderId,
                command.canceledBy(),
                command.reason()
        );
        
        OrderResponse response = commandService.cancelOrder(updatedCommand);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/ship")
    public ResponseEntity<OrderResponse> shipOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody ShipOrderCommand command) {
        log.info("Received ShipOrder request for order {}", orderId);
        
        ShipOrderCommand updatedCommand = new ShipOrderCommand(
                orderId,
                command.trackingNumber(),
                command.carrier()
        );
        
        OrderResponse response = commandService.shipOrder(updatedCommand);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderResponse> addItem(
            @PathVariable UUID orderId,
            @Valid @RequestBody AddItemCommand command) {
        log.info("Received AddItem request for order {}", orderId);
        
        AddItemCommand updatedCommand = new AddItemCommand(
                orderId,
                command.sku(),
                command.productName(),
                command.quantity(),
                command.unitPrice()
        );
        
        OrderResponse response = commandService.addItem(updatedCommand);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{orderId}/items/{sku}")
    public ResponseEntity<OrderResponse> removeItem(
            @PathVariable UUID orderId,
            @PathVariable String sku) {
        log.info("Received RemoveItem request for order {} and sku {}", orderId, sku);
        
        RemoveItemCommand command = new RemoveItemCommand(orderId, sku);
        OrderResponse response = commandService.removeItem(command);
        return ResponseEntity.ok(response);
    }
}
