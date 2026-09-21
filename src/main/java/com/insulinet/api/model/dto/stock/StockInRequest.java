package com.insulinet.api.model.dto.stock;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockInRequest(
        @NotNull @Positive Integer containers
) {
}
