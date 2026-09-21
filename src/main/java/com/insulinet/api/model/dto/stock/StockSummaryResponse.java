package com.insulinet.api.model.dto.stock;

import java.math.BigDecimal;

public record StockSummaryResponse(
        Long insulinId,
        String insulinName,
        BigDecimal currentStockUnits
) {
}
