package com.orderplatform.command.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "command_deduplication")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandDeduplicationEntity {

    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "command_type", nullable = false, length = 100)
    private String commandType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> response;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
