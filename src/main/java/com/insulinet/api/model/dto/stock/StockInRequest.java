package com.insulinet.api.model.dto.stock;

import jakarta.validation.constraints.Positive;

public record StockInRequest(
        @Positive Integer containers
) {
}
