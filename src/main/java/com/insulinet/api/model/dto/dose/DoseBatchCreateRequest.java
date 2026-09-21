package com.insulinet.api.model.dto.dose;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DoseBatchCreateRequest(
        @NotEmpty @Valid List<DoseBatchItemRequest> doses
) {
}
