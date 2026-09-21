package com.insulinet.api.service;

import com.insulinet.api.model.dto.insulin.InsulinSummaryResponse;
import com.insulinet.api.model.entity.Insulin;
import com.insulinet.api.model.entity.StockMovement;
import com.insulinet.api.model.enums.MovementType;
import com.insulinet.api.repository.StockMovementRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Espelha app/services/projection_service.py: media diaria de consumo dos
 * ultimos (ate) 14 dias distintos registrados dentro de uma janela de 30
 * dias, exigindo pelo menos 3 dias com dados para projetar.
 */
@Service
public class ProjectionService {

    private static final int LOOKBACK_DAYS = 30;
    private static final int MAX_HISTORY_DAYS = 14;
    private static final int MIN_HISTORY_DAYS = 3;

    private final StockMovementRepository stockMovementRepository;
    private final StockService stockService;
    private final ZoneId appTimeZone;

    public ProjectionService(
            StockMovementRepository stockMovementRepository,
            StockService stockService,
            ZoneId appTimeZone
    ) {
        this.stockMovementRepository = stockMovementRepository;
        this.stockService = stockService;
        this.appTimeZone = appTimeZone;
    }

    public InsulinSummaryResponse buildInsulinSummary(Insulin insulin) {
        BigDecimal currentStock = stockService.calculateCurrentStock(insulin.getId());

        ZonedDateTime nowLocal = ZonedDateTime.now(appTimeZone);
        LocalDate todayLocal = nowLocal.toLocalDate();
        LocalDate lookbackDate = todayLocal.minusDays(LOOKBACK_DAYS);
        Instant lookbackUtc = lookbackDate.atStartOfDay(appTimeZone).toInstant();

        List<StockMovement> doseMovements = stockMovementRepository
                .findByInsulinIdAndMovementTypeAndOccurredAtGreaterThanEqualOrderByOccurredAtAsc(
                        insulin.getId(), MovementType.DOSE, lookbackUtc);

        Map<LocalDate, BigDecimal> dailyConsumption = new HashMap<>();

        for (StockMovement movement : doseMovements) {
            LocalDate movementDate = movement.getOccurredAt().atZone(appTimeZone).toLocalDate();

            if (!movementDate.isBefore(todayLocal)) {
                continue;
            }

            BigDecimal usedUnits = movement.getQuantityUnits().abs();
            dailyConsumption.merge(movementDate, usedUnits, BigDecimal::add);
        }

        List<LocalDate> recordedDays = dailyConsumption.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .toList();
        List<LocalDate> selectedDays = recordedDays.stream().limit(MAX_HISTORY_DAYS).toList();
        int historyDaysUsed = selectedDays.size();

        if (historyDaysUsed < MIN_HISTORY_DAYS) {
            return new InsulinSummaryResponse(
                    insulin.getId(), insulin.getName(), currentStock,
                    null, historyDaysUsed, null, null, false);
        }

        BigDecimal totalConsumption = selectedDays.stream()
                .map(dailyConsumption::get)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageDailyConsumption = totalConsumption
                .divide(BigDecimal.valueOf(historyDaysUsed), 2, RoundingMode.HALF_UP);

        if (averageDailyConsumption.compareTo(BigDecimal.ZERO) <= 0
                || currentStock.compareTo(BigDecimal.ZERO) <= 0) {
            return new InsulinSummaryResponse(
                    insulin.getId(), insulin.getName(), currentStock,
                    averageDailyConsumption, historyDaysUsed, null, null, false);
        }

        BigDecimal estimatedDaysRemaining = currentStock
                .divide(averageDailyConsumption, 1, RoundingMode.HALF_UP);

        long wholeDays = estimatedDaysRemaining.setScale(0, RoundingMode.DOWN).longValue();
        LocalDate estimatedEndDate = todayLocal.plusDays(wholeDays);

        return new InsulinSummaryResponse(
                insulin.getId(), insulin.getName(), currentStock,
                averageDailyConsumption, historyDaysUsed, estimatedDaysRemaining, estimatedEndDate, true);
    }
}
