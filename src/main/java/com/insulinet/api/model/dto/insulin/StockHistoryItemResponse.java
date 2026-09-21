package com.insulinet.api.model.dto.insulin;

import java.math.BigDecimal;
import java.time.Instant;

public record StockHistoryItemResponse(
        Long id,
        Long containerId,
        Long groupId,
        String movementType,
        BigDecimal quantityUnits,
        Instant occurredAt,
        String notes,
        boolean occurredTimeKnown
) {
}
