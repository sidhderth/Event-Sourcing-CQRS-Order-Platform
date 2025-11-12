package com.orderplatform.domain;

import com.orderplatform.domain.events.*;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order aggregate root that enforces business invariants.
 */
@Getter
public class Order {
    
    private UUID orderId;
    private UUID customerId;
    private OrderStatus status;
    private List<OrderItem> items;
    private Money totalAmount;
    private String currency;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;
    
    public Order() {
        this.items = new ArrayList<>();
        this.version = 0;
    }
    
    /**
     * Creates a new order from a command.
     */
    public OrderCreatedEvent create(UUID orderId, UUID customerId, List<OrderItem> items, String currency) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        
        Instant now = Instant.now();
        OrderCreatedEvent event = new OrderCreatedEvent(
            UUID.randomUUID(),
            orderId,
            1L,
            now,
            customerId,
            new ArrayList<>(items),
            currency
        );
        
        apply(event);
        return event;
    }
    
    /**
     * Approves the order.
     */
    public OrderApprovedEvent approve(UUID approvedBy, String reason) {
        if (status != OrderStatus.CREATED) {
            throw new InvalidOrderStateException(
                String.format("Cannot approve order in %s status", status));
        }
        if (approvedBy == null) {
            throw new IllegalArgumentException("Approved by cannot be null");
        }
        
        Instant now = Instant.now();
        OrderApprovedEvent event = new OrderApprovedEvent(
            UUID.randomUUID(),
            orderId,
            version + 1,
            now,
            approvedBy,
            reason
        );
        
        apply(event);
        return event;
    }
    
    /**
     * Rejects the order.
     */
    public OrderRejectedEvent reject(UUID rejectedBy, String reason) {
        if (status != OrderStatus.CREATED) {
            throw new InvalidOrderStateException(
                String.format("Cannot reject order in %s status", status));
        }
        if (rejectedBy == null) {
            throw new IllegalArgumentException("Rejected by cannot be null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        
        Instant now = Instant.now();
        OrderRejectedEvent event = new OrderRejectedEvent(
            UUID.randomUUID(),
            orderId,
            version + 1,
            now,
            rejectedBy,
            reason
        );
        
        apply(event);
        return event;
    }
    
    /**
     * Cancels the order.
     */
    public OrderCanceledEvent cancel(UUID canceledBy, String reason) {
        if (status == OrderStatus.SHIPPED) {
            throw new InvalidOrderStateException("Cannot cancel shipped order");
        }
        if (status == OrderStatus.CANCELED) {
            throw new InvalidOrderStateException("Order is already canceled");
        }
        if (canceledBy == null) {
            throw new IllegalArgumentException("Canceled by cannot be null");
        }
        
        Instant now = Instant.now();
        OrderCanceledEvent event = new OrderCanceledEvent(
            UUID.randomUUID(),
            orderId,
            version + 1,
            now,
            canceledBy,
            reason
        );
        
        apply(event);
        return event;
    }
    
    /**
     * Ships the order.
     */
    public OrderShippedEvent ship(String trackingNumber, String carrier) {
        if (status != OrderStatus.APPROVED) {
            throw new InvalidOrderStateException(
                String.format("Cannot ship order in %s status. Order must be APPROVED", status));
        }
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new IllegalArgumentException("Tracking number is required");
        }
        if (carrier == null || carrier.isBlank()) {
            throw new IllegalArgumentException("Carrier is required");
        }
        
        Instant now = Instant.now();
        OrderShippedEvent event = new OrderShippedEvent(
            UUID.randomUUID(),
            orderId,
            version + 1,
            now,
            trackingNumber,
            carrier
        );
        
        apply(event);
        return event;
    }
    
    /**
     * Adds an item to the order.
     */
    public ItemAddedEvent addItem(OrderItem item) {
        if (status != OrderStatus.CREATED) {
            throw new InvalidOrderStateException(
                String.format("Cannot add items to order in %s status", status));
        }
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        if (!item.unitPrice().currency().equals(currency)) {
            throw new IllegalArgumentException(
                String.format("Item currency %s does not match order currency %s", 
                    item.unitPrice().currency(), currency));
        }
        
        Instant now = Instant.now();
        ItemAddedEvent event = new ItemAddedEvent(
            UUID.randomUUID(),
            orderId,
            version + 1,
            now,
            item
        );
        
        apply(event);
        return event;
    }
    
    /**
     * Removes an item from the order.
     */
    public ItemRemovedEvent removeItem(String sku) {
        if (status != OrderStatus.CREATED) {
            throw new InvalidOrderStateException(
                String.format("Cannot remove items from order in %s status", status));
        }
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be null or empty");
        }
        
        boolean found = items.stream().anyMatch(item -> item.sku().equals(sku));
        if (!found) {
            throw new IllegalArgumentException(
                String.format("Item with SKU %s not found in order", sku));
        }
        
        Instant now = Instant.now();
        ItemRemovedEvent event = new ItemRemovedEvent(
            UUID.randomUUID(),
            orderId,
            version + 1,
            now,
            sku
        );
        
        apply(event);
        return event;
    }
    
    /**
     * Applies an event to mutate the aggregate state.
     */
    public void apply(DomainEvent event) {
        switch (event) {
            case OrderCreatedEvent e -> applyOrderCreated(e);
            case OrderApprovedEvent e -> applyOrderApproved(e);
            case OrderRejectedEvent e -> applyOrderRejected(e);
            case OrderCanceledEvent e -> applyOrderCanceled(e);
            case OrderShippedEvent e -> applyOrderShipped(e);
            case ItemAddedEvent e -> applyItemAdded(e);
            case ItemRemovedEvent e -> applyItemRemoved(e);
            default -> throw new IllegalArgumentException("Unknown event type: " + event.getClass());
        }
    }
    
    private void applyOrderCreated(OrderCreatedEvent event) {
        this.orderId = event.getAggregateId();
        this.customerId = event.getCustomerId();
        this.status = OrderStatus.CREATED;
        this.items = new ArrayList<>(event.getItems());
        this.currency = event.getCurrency();
        this.totalAmount = calculateTotal();
        this.version = event.getVersion();
        this.createdAt = event.getOccurredAt();
        this.updatedAt = event.getOccurredAt();
    }
    
    private void applyOrderApproved(OrderApprovedEvent event) {
        this.status = OrderStatus.APPROVED;
        this.version = event.getVersion();
        this.updatedAt = event.getOccurredAt();
    }
    
    private void applyOrderRejected(OrderRejectedEvent event) {
        this.status = OrderStatus.REJECTED;
        this.version = event.getVersion();
        this.updatedAt = event.getOccurredAt();
    }
    
    private void applyOrderCanceled(OrderCanceledEvent event) {
        this.status = OrderStatus.CANCELED;
        this.version = event.getVersion();
        this.updatedAt = event.getOccurredAt();
    }
    
    private void applyOrderShipped(OrderShippedEvent event) {
        this.status = OrderStatus.SHIPPED;
        this.version = event.getVersion();
        this.updatedAt = event.getOccurredAt();
    }
    
    private void applyItemAdded(ItemAddedEvent event) {
        this.items.add(event.getItem());
        this.totalAmount = calculateTotal();
        this.version = event.getVersion();
        this.updatedAt = event.getOccurredAt();
    }
    
    private void applyItemRemoved(ItemRemovedEvent event) {
        this.items.removeIf(item -> item.sku().equals(event.getSku()));
        this.totalAmount = calculateTotal();
        this.version = event.getVersion();
        this.updatedAt = event.getOccurredAt();
    }
    
    private Money calculateTotal() {
        if (items.isEmpty()) {
            return new Money(java.math.BigDecimal.ZERO, currency);
        }
        return items.stream()
            .map(OrderItem::lineTotal)
            .reduce(Money::add)
            .orElseThrow();
    }
}
