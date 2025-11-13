package com.orderplatform.query.readmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReadModel {

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("items")
    @Builder.Default
    private List<OrderItemReadModel> items = new ArrayList<>();

    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("version")
    private Long version;

    @JsonProperty("approvedBy")
    private String approvedBy;

    @JsonProperty("rejectionReason")
    private String rejectionReason;

    @JsonProperty("trackingNumber")
    private String trackingNumber;

    @JsonProperty("carrier")
    private String carrier;
}
