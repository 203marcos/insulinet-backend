package com.insulinet.api.model.dto.stock;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record StockAdjustmentRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal actualStockUnits,
        @NotBlank @Size(min = 3, max = 500) String notes
) {
}
