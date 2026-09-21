package com.insulinet.api.service;

import com.insulinet.api.model.dto.insulin.InsulinSummaryResponse;
import com.insulinet.api.model.entity.Insulin;
import com.insulinet.api.model.entity.InsulinContainer;
import com.insulinet.api.model.entity.StockMovement;
import com.insulinet.api.model.enums.MovementType;
import com.insulinet.api.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectionServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");

    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private StockService stockService;

    private ProjectionService projectionService;
    private Insulin insulin;

    @BeforeEach
    void setUp() {
        projectionService = new ProjectionService(stockMovementRepository, stockService, ZONE);
        insulin = new Insulin();
        insulin.setId(1L);
        insulin.setName("Lantus");
    }

    private StockMovement doseOn(java.time.LocalDate date, LocalTime time, String units) {
        InsulinContainer container = new InsulinContainer();
        container.setId(1L);

        StockMovement movement = new StockMovement();
        movement.setContainer(container);
        movement.setMovementType(MovementType.DOSE);
        movement.setQuantityUnits(new BigDecimal(units).negate());
        movement.setOccurredAt(date.atTime(time).atZone(ZONE).toInstant());
        return movement;
    }

    @Test
    void fewerThanThreeDistinctDaysMeansNoProjection() {
        ZonedDateTime today = ZonedDateTime.now(ZONE);
        when(stockService.calculateCurrentStock(1L)).thenReturn(new BigDecimal("50.00"));
        when(stockMovementRepository.findByInsulinIdAndMovementTypeAndOccurredAtGreaterThanEqualOrderByOccurredAtAsc(
                any(), any(), any())).thenReturn(List.of(
                doseOn(today.toLocalDate().minusDays(1), LocalTime.NOON, "10.00"),
                doseOn(today.toLocalDate().minusDays(2), LocalTime.NOON, "10.00")
        ));

        InsulinSummaryResponse summary = projectionService.buildInsulinSummary(insulin);

        assertThat(summary.projectionAvailable()).isFalse();
        assertThat(summary.historyDaysUsed()).isEqualTo(2);
        assertThat(summary.averageDailyConsumptionUnits()).isNull();
        assertThat(summary.estimatedDaysRemaining()).isNull();
    }

    @Test
    void movementsFromTodayAreExcludedFromTheAverage() {
        ZonedDateTime today = ZonedDateTime.now(ZONE);
        when(stockService.calculateCurrentStock(1L)).thenReturn(new BigDecimal("50.00"));
        List<StockMovement> movements = new ArrayList<>(List.of(
                doseOn(today.toLocalDate(), LocalTime.NOON, "999.00"),
                doseOn(today.toLocalDate().minusDays(1), LocalTime.NOON, "10.00"),
                doseOn(today.toLocalDate().minusDays(2), LocalTime.NOON, "10.00"),
                doseOn(today.toLocalDate().minusDays(3), LocalTime.NOON, "10.00")
        ));
        when(stockMovementRepository.findByInsulinIdAndMovementTypeAndOccurredAtGreaterThanEqualOrderByOccurredAtAsc(
                any(), any(), any())).thenReturn(movements);

        InsulinSummaryResponse summary = projectionService.buildInsulinSummary(insulin);

        assertThat(summary.historyDaysUsed()).isEqualTo(3);
        assertThat(summary.averageDailyConsumptionUnits()).isEqualByComparingTo("10.00");
    }

    @Test
    void estimatedDaysRemainingRoundsHalfUpAndEndDateTruncates() {
        ZonedDateTime today = ZonedDateTime.now(ZONE);
        // media diaria = 3.00 U/dia (3 dias de 3.00 cada); estoque = 10.00
        // -> 10.00 / 3.00 = 3.333... -> arredonda HALF_UP para 3.3 dias
        // -> data final trunca para hoje + 3 dias (nao 3.3 arredondado pra 3... ja e 3)
        when(stockService.calculateCurrentStock(1L)).thenReturn(new BigDecimal("10.00"));
        List<StockMovement> movements = List.of(
                doseOn(today.toLocalDate().minusDays(1), LocalTime.NOON, "3.00"),
                doseOn(today.toLocalDate().minusDays(2), LocalTime.NOON, "3.00"),
                doseOn(today.toLocalDate().minusDays(3), LocalTime.NOON, "3.00")
        );
        when(stockMovementRepository.findByInsulinIdAndMovementTypeAndOccurredAtGreaterThanEqualOrderByOccurredAtAsc(
                any(), any(), any())).thenReturn(movements);

        InsulinSummaryResponse summary = projectionService.buildInsulinSummary(insulin);

        assertThat(summary.projectionAvailable()).isTrue();
        assertThat(summary.averageDailyConsumptionUnits()).isEqualByComparingTo("3.00");
        assertThat(summary.estimatedDaysRemaining()).isEqualByComparingTo("3.3");
        assertThat(summary.estimatedEndDate()).isEqualTo(today.toLocalDate().plusDays(3));
    }

    @Test
    void zeroCurrentStockMeansNoProjectionEvenWithEnoughHistory() {
        ZonedDateTime today = ZonedDateTime.now(ZONE);
        when(stockService.calculateCurrentStock(1L)).thenReturn(BigDecimal.ZERO);
        List<StockMovement> movements = List.of(
                doseOn(today.toLocalDate().minusDays(1), LocalTime.NOON, "3.00"),
                doseOn(today.toLocalDate().minusDays(2), LocalTime.NOON, "3.00"),
                doseOn(today.toLocalDate().minusDays(3), LocalTime.NOON, "3.00")
        );
        when(stockMovementRepository.findByInsulinIdAndMovementTypeAndOccurredAtGreaterThanEqualOrderByOccurredAtAsc(
                any(), any(), any())).thenReturn(movements);

        InsulinSummaryResponse summary = projectionService.buildInsulinSummary(insulin);

        assertThat(summary.projectionAvailable()).isFalse();
        assertThat(summary.estimatedDaysRemaining()).isNull();
    }

    @Test
    void onlyTheFourteenMostRecentDistinctDaysAreConsidered() {
        ZonedDateTime today = ZonedDateTime.now(ZONE);
        when(stockService.calculateCurrentStock(1L)).thenReturn(new BigDecimal("100.00"));

        List<StockMovement> movements = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            // dias mais antigos com consumo muito maior: se entrassem na media,
            // o resultado mudaria bastante - o teste garante que sao ignorados.
            String units = i > 14 ? "1000.00" : "2.00";
            movements.add(doseOn(today.toLocalDate().minusDays(i), LocalTime.NOON, units));
        }
        when(stockMovementRepository.findByInsulinIdAndMovementTypeAndOccurredAtGreaterThanEqualOrderByOccurredAtAsc(
                any(), any(), any())).thenReturn(movements);

        InsulinSummaryResponse summary = projectionService.buildInsulinSummary(insulin);

        assertThat(summary.historyDaysUsed()).isEqualTo(14);
        assertThat(summary.averageDailyConsumptionUnits()).isEqualByComparingTo("2.00");
    }
}
