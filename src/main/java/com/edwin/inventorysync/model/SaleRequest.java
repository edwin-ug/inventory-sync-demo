package com.edwin.inventorysync.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleRequest(
        @NotNull Channel channel,
        @NotNull String sku,
        @Min(1) int quantity
) {
}
