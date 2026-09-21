package com.insulinet.api.model.dto.insulin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InsulinSummaryResponse(
        Long insulinId,
        String insulinName,
        BigDecimal currentStockUnits,
        BigDecimal averageDailyConsumptionUnits,
        int historyDaysUsed,
        BigDecimal estimatedDaysRemaining,
        LocalDate estimatedEndDate,
        boolean projectionAvailable
) {
}
