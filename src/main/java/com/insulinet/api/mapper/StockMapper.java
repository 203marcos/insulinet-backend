package com.insulinet.api.mapper;

import com.insulinet.api.model.dto.stock.InsulinContainerResponse;
import com.insulinet.api.model.dto.stock.StockMovementResponse;
import com.insulinet.api.model.entity.InsulinContainer;
import com.insulinet.api.model.entity.StockMovement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StockMapper {

    public StockMovementResponse toResponse(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getInsulin().getId(),
                movement.getContainer().getId(),
                movement.getGroupId(),
                movement.getMovementType().name(),
                movement.getQuantityUnits(),
                movement.getOccurredAt(),
                movement.getNotes(),
                movement.getCreatedAt(),
                movement.isOccurredTimeKnown()
        );
    }

    public InsulinContainerResponse toResponse(InsulinContainer container, BigDecimal remainingUnits) {
        return new InsulinContainerResponse(
                container.getId(),
                container.getInsulin().getId(),
                container.getStatus().name(),
                container.getInitialUnits(),
                remainingUnits,
                container.getOpenedAt(),
                container.getCreatedAt()
        );
    }
}
