package com.insulinet.api.model.dto.stock;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StockInUpdateRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal units
) {
}
