package com.insulinet.api.service;

import com.insulinet.api.exception.BadRequestException;
import com.insulinet.api.repository.StockMovementRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class DoseServiceResolveDateTimeTest {

    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private StockService stockService;
    @Mock
    private ContainerService containerService;
    @Mock
    private InsulinService insulinService;
    @Mock
    private EntityManager entityManager;

    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");

    private DoseService doseService;

    @BeforeEach
    void setUp() {
        doseService = new DoseService(
                stockMovementRepository, stockService, containerService, insulinService, ZONE, entityManager);
    }

    @Test
    void noDateDefaultsToNowWithKnownTime() {
        var resolved = doseService.resolveDoseDateTime(null, null);

        assertThat(resolved.timeKnown()).isTrue();
        assertThat(resolved.occurredAt()).isNotNull();
    }

    @Test
    void futureDateIsRejected() {
        LocalDate tomorrow = ZonedDateTime.now(ZONE).toLocalDate().plusDays(1);

        assertThatThrownBy(() -> doseService.resolveDoseDateTime(tomorrow, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    void futureTimeOnCurrentDateIsRejected() {
        ZonedDateTime nowLocal = ZonedDateTime.now(ZONE);
        LocalTime future = nowLocal.toLocalTime().plusHours(2).withNano(0);
        // evita falso positivo se plusHours(2) virar o dia
        if (future.isBefore(nowLocal.toLocalTime())) {
            return;
        }

        assertThatThrownBy(() -> doseService.resolveDoseDateTime(nowLocal.toLocalDate(), future))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    void pastDateWithoutTimeDefaultsToNoonLocalAndTimeUnknown() {
        LocalDate yesterday = ZonedDateTime.now(ZONE).toLocalDate().minusDays(1);

        var resolved = doseService.resolveDoseDateTime(yesterday, null);

        assertThat(resolved.timeKnown()).isFalse();
        ZonedDateTime resolvedLocal = resolved.occurredAt().atZone(ZONE);
        assertThat(resolvedLocal.toLocalDate()).isEqualTo(yesterday);
        assertThat(resolvedLocal.toLocalTime()).isEqualTo(LocalTime.NOON);
    }

    @Test
    void pastDateWithExplicitTimeIsKnown() {
        LocalDate yesterday = ZonedDateTime.now(ZONE).toLocalDate().minusDays(1);
        LocalTime time = LocalTime.of(8, 30);

        var resolved = doseService.resolveDoseDateTime(yesterday, time);

        assertThat(resolved.timeKnown()).isTrue();
        ZonedDateTime resolvedLocal = resolved.occurredAt().atZone(ZONE);
        assertThat(resolvedLocal.toLocalDate()).isEqualTo(yesterday);
        assertThat(resolvedLocal.toLocalTime()).isEqualTo(time);
    }
}
