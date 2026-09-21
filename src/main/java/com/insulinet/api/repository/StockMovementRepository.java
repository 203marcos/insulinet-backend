package com.insulinet.api.repository;

import com.insulinet.api.model.entity.StockMovement;
import com.insulinet.api.model.enums.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByInsulinIdOrderByOccurredAtDescIdDesc(Long insulinId);

    List<StockMovement> findByContainerId(Long containerId);

    Optional<StockMovement> findByIdAndInsulinIdAndMovementType(
            Long id, Long insulinId, MovementType movementType);

    @Query("""
            SELECT m FROM StockMovement m
            WHERE m.insulin.id = :insulinId
              AND m.movementType = :movementType
              AND (m.id = :groupKey OR m.groupId = :groupKey)
            """)
    List<StockMovement> findGroup(
            @Param("insulinId") Long insulinId,
            @Param("movementType") MovementType movementType,
            @Param("groupKey") Long groupKey);

    @Query("""
            SELECT COALESCE(SUM(m.quantityUnits), 0) FROM StockMovement m
            WHERE m.insulin.id = :insulinId
            """)
    BigDecimal sumQuantityByInsulinId(@Param("insulinId") Long insulinId);

    @Query("""
            SELECT COALESCE(SUM(m.quantityUnits), 0) FROM StockMovement m
            WHERE m.container.id = :containerId
            """)
    BigDecimal sumQuantityByContainerId(@Param("containerId") Long containerId);

    List<StockMovement> findByInsulinIdAndMovementTypeAndOccurredAtGreaterThanEqualOrderByOccurredAtAsc(
            Long insulinId, MovementType movementType, Instant occurredAtFrom);
}
