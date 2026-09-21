package com.insulinet.api.model.dto.insulin;

import java.math.BigDecimal;
import java.time.Instant;

public record InsulinResponse(
        Long id,
        String name,
        BigDecimal concentrationUnitsPerMl,
        BigDecimal containerVolumeMl,
        boolean active,
        Instant createdAt
) {
}
