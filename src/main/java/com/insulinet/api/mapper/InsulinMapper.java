package com.insulinet.api.mapper;

import com.insulinet.api.model.dto.insulin.InsulinResponse;
import com.insulinet.api.model.dto.insulin.StockHistoryItemResponse;
import com.insulinet.api.model.entity.Insulin;
import com.insulinet.api.model.entity.StockMovement;
import org.springframework.stereotype.Component;

@Component
public class InsulinMapper {

    public InsulinResponse toResponse(Insulin insulin) {
        return new InsulinResponse(
                insulin.getId(),
                insulin.getName(),
                insulin.getConcentrationUnitsPerMl(),
                insulin.getContainerVolumeMl(),
                insulin.isActive(),
                insulin.getCreatedAt()
        );
    }

    public StockHistoryItemResponse toHistoryItem(StockMovement movement) {
        return new StockHistoryItemResponse(
                movement.getId(),
                movement.getContainer().getId(),
                movement.getGroupId(),
                movement.getMovementType().name(),
                movement.getQuantityUnits(),
                movement.getOccurredAt(),
                movement.getNotes(),
                movement.isOccurredTimeKnown()
        );
    }
}
