package com.insulinet.api.model.dto.stock;

import java.math.BigDecimal;
import java.time.Instant;

public record StockMovementResponse(
        Long id,
        Long insulinId,
        Long containerId,
        Long groupId,
        String movementType,
        BigDecimal quantityUnits,
        Instant occurredAt,
        String notes,
        Instant createdAt,
        boolean occurredTimeKnown
) {
}
