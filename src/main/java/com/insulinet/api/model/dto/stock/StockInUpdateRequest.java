package com.insulinet.api.model.dto.stock;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record StockInUpdateRequest(
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal units
) {
}
