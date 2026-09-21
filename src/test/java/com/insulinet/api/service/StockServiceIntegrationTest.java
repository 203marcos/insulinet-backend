package com.insulinet.api.service;

import com.insulinet.api.AbstractIntegrationTest;
import com.insulinet.api.exception.ConflictException;
import com.insulinet.api.model.entity.Insulin;
import com.insulinet.api.model.entity.InsulinContainer;
import com.insulinet.api.model.entity.StockMovement;
import com.insulinet.api.model.entity.User;
import com.insulinet.api.model.enums.ContainerStatus;
import com.insulinet.api.repository.InsulinRepository;
import com.insulinet.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa a distribuicao FIFO entre containers (a regra de negocio mais
 * sensivel da migracao) diretamente na camada de service, contra um
 * Postgres real via Testcontainers.
 */
class StockServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private StockService stockService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InsulinRepository insulinRepository;

    private User user;
    private Insulin insulin;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("Teste FIFO");
        user.setEmail("fifo-" + System.nanoTime() + "@example.com");
        user.setPasswordHash("hash");
        user.setCreatedAt(Instant.now());
        user = userRepository.save(user);

        insulin = new Insulin();
        insulin.setUser(user);
        insulin.setName("Insulina Teste");
        insulin.setConcentrationUnitsPerMl(new BigDecimal("100.00"));
        insulin.setContainerVolumeMl(new BigDecimal("3.00"));
        insulin.setActive(true);
        insulin.setCreatedAt(Instant.now());
        insulin = insulinRepository.save(insulin);
    }

    @Test
    void distributesAcrossMultipleContainersInFifoOrderAndGroupsMovements() {
        stockService.createStockInContainers(insulin, 3); // 300.00 U cada, total 900.00

        List<StockMovement> doseMovements = stockService.distributeNegativeDelta(
                insulin, new BigDecimal("450.00"),
                com.insulinet.api.model.enums.MovementType.DOSE,
                Instant.now(), true, "dose grande");

        assertThat(doseMovements).hasSize(2);
        assertThat(doseMovements.get(0).getQuantityUnits()).isEqualByComparingTo("-300.00");
        assertThat(doseMovements.get(1).getQuantityUnits()).isEqualByComparingTo("-150.00");
        assertThat(doseMovements.get(1).getGroupId()).isEqualTo(doseMovements.get(0).getId());

        assertThat(stockService.calculateCurrentStock(insulin.getId())).isEqualByComparingTo("450.00");
    }

    @Test
    void doseThatExactlyFitsOneContainerCreatesNoGroup() {
        stockService.createStockInContainers(insulin, 1); // 300.00 U

        List<StockMovement> movements = stockService.distributeNegativeDelta(
                insulin, new BigDecimal("300.00"),
                com.insulinet.api.model.enums.MovementType.DOSE,
                Instant.now(), true, null);

        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).getGroupId()).isNull();
    }

    @Test
    void insufficientStockAcrossAllContainersThrows() {
        stockService.createStockInContainers(insulin, 1); // 300.00 U

        assertThatThrownBy(() -> stockService.distributeNegativeDelta(
                insulin, new BigDecimal("500.00"),
                com.insulinet.api.model.enums.MovementType.DOSE,
                Instant.now(), true, null))
                .isInstanceOf(com.insulinet.api.exception.InsufficientStockException.class);
    }

    @Test
    void adjustStockWithNoContainersAvailableIsConflict() {
        assertThatThrownBy(() -> stockService.adjustStock(user, insulin.getId(), new BigDecimal("10.00"), "ajuste"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void discardingAnOpenContainerCreatesADiscardMovementForItsRemainingBalance() {
        stockService.createStockInContainers(insulin, 1);
        stockService.distributeNegativeDelta(
                insulin, new BigDecimal("100.00"),
                com.insulinet.api.model.enums.MovementType.DOSE,
                Instant.now(), true, null); // sobra 200.00 no container

        List<InsulinContainer> containers = stockService.getContainers(user, insulin.getId());
        Long containerId = containers.get(0).getId();

        InsulinContainer discarded = stockService.discardContainer(user, insulin.getId(), containerId);

        assertThat(discarded.getStatus()).isEqualTo(ContainerStatus.DISCARDED);
        assertThat(stockService.calculateCurrentStock(insulin.getId())).isEqualByComparingTo("0.00");
    }
}
