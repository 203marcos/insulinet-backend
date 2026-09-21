package com.insulinet.api.model.dto.dose;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record DoseCreateRequest(
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal units,
        LocalDate occurredDate,
        LocalTime occurredTime,
        @Size(max = 500) String notes
) {
}
