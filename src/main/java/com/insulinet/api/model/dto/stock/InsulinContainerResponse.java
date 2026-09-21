package com.insulinet.api.model.dto.stock;

import java.math.BigDecimal;
import java.time.Instant;

public record InsulinContainerResponse(
        Long id,
        Long insulinId,
        String status,
        BigDecimal initialUnits,
        BigDecimal remainingUnits,
        Instant openedAt,
        Instant createdAt
) {
}
