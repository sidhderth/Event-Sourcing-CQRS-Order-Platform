package com.orderplatform.query.readmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemReadModel {

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("unitPrice")
    private BigDecimal unitPrice;

    @JsonProperty("lineTotal")
    private BigDecimal lineTotal;
}
