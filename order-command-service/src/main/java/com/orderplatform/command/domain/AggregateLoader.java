package com.orderplatform.command.domain;

import com.orderplatform.domain.Order;
import com.orderplatform.domain.events.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AggregateLoader {

    private final EventStoreRepository eventStoreRepository;
    private final SnapshotRepository snapshotRepository;

    @Value("${app.snapshot.interval:50}")
    private int snapshotInterval;

    /**
     * Loads an Order aggregate by replaying events from the event store.
     * Uses snapshots for optimization when available.
     *
     * @param aggregateId The ID of the aggregate to load
     * @return Optional containing the loaded Order, or empty if not found
     */
    public Optional<Order> loadAggregate(UUID aggregateId) {
        // Try to load from snapshot first
        Optional<Order> snapshotOpt = snapshotRepository.findByAggregateId(aggregateId);
        
        Order order;
        long startVersion;
        
        if (snapshotOpt.isPresent()) {
            order = snapshotOpt.get();
            startVersion = order.getVersion() + 1;
            log.debug("Loaded snapshot for aggregate {} at version {}", aggregateId, order.getVersion());
        } else {
            order = new Order();
            startVersion = 1;
            log.debug("No snapshot found for aggregate {}, starting from scratch", aggregateId);
        }

        // Load events after the snapshot
        List<DomainEvent> events = eventStoreRepository.findByAggregateId(aggregateId);
        
        // Filter events that come after the snapshot
        List<DomainEvent> eventsToReplay = events.stream()
                .filter(e -> e.getVersion() >= startVersion)
                .toList();

        if (eventsToReplay.isEmpty() && snapshotOpt.isEmpty()) {
            log.debug("No events found for aggregate {}", aggregateId);
            return Optional.empty();
        }

        // Replay events to reconstruct state
        for (DomainEvent event : eventsToReplay) {
            order.apply(event);
        }

        log.debug("Loaded aggregate {} with {} events replayed, final version: {}", 
                aggregateId, eventsToReplay.size(), order.getVersion());

        return Optional.of(order);
    }

    /**
     * Saves an event and creates a snapshot if the snapshot interval is reached.
     *
     * @param event The event to save
     * @param order The current state of the aggregate
     */
    public void saveEventAndSnapshot(DomainEvent event, Order order) {
        // Append event to event store
        eventStoreRepository.append(event);

        // Create snapshot if interval is reached
        if (shouldCreateSnapshot(order.getVersion())) {
            snapshotRepository.save(order.getOrderId(), order);
            log.info("Created snapshot for aggregate {} at version {}", 
                    order.getOrderId(), order.getVersion());
        }
    }

    /**
     * Determines if a snapshot should be created based on the version number.
     *
     * @param version The current version of the aggregate
     * @return true if a snapshot should be created
     */
    private boolean shouldCreateSnapshot(long version) {
        return version % snapshotInterval == 0;
    }

    /**
     * Rebuilds snapshots for a specific aggregate by replaying all events.
     * Used for maintenance and recovery operations.
     *
     * @param aggregateId The ID of the aggregate to rebuild snapshots for
     */
    public void rebuildSnapshots(UUID aggregateId) {
        log.info("Rebuilding snapshots for aggregate {}", aggregateId);
        
        // Delete existing snapshot
        snapshotRepository.deleteByAggregateId(aggregateId);

        // Load all events
        List<DomainEvent> events = eventStoreRepository.findByAggregateId(aggregateId);
        
        if (events.isEmpty()) {
            log.warn("No events found for aggregate {}", aggregateId);
            return;
        }

        // Replay events and create snapshots at intervals
        Order order = new Order();
        for (DomainEvent event : events) {
            order.apply(event);
            
            if (shouldCreateSnapshot(order.getVersion())) {
                snapshotRepository.save(aggregateId, order);
                log.debug("Created snapshot at version {}", order.getVersion());
            }
        }

        log.info("Completed rebuilding snapshots for aggregate {}, final version: {}", 
                aggregateId, order.getVersion());
    }
}
