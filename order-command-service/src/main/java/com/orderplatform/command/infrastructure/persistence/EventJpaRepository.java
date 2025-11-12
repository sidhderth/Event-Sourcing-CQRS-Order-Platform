package com.orderplatform.command.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventJpaRepository extends JpaRepository<EventEntity, UUID> {

    List<EventEntity> findByAggregateIdOrderByVersionAsc(UUID aggregateId);

    @Query("SELECT e FROM EventEntity e WHERE e.occurredAt >= :fromTime ORDER BY e.occurredAt ASC")
    List<EventEntity> findAllOrderByOccurredAt(@Param("fromTime") Instant fromTime);

    @Query("SELECT e FROM EventEntity e WHERE e.occurredAt BETWEEN :fromTime AND :toTime ORDER BY e.occurredAt ASC")
    List<EventEntity> findByOccurredAtBetween(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);
}
