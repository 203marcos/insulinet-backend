package com.insulinet.api.model.dto.insulin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InsulinCreateRequest(
        @NotBlank @Size(min = 1, max = 100) String name,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal concentrationUnitsPerMl,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal containerVolumeMl
) {
}
