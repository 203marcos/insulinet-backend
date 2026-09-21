package com.insulinet.api.service;

import com.insulinet.api.model.entity.InsulinContainer;
import com.insulinet.api.model.enums.ContainerStatus;
import com.insulinet.api.repository.InsulinContainerRepository;
import com.insulinet.api.repository.StockMovementRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContainerServiceTest {

    @Mock
    private InsulinContainerRepository containerRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private EntityManager entityManager;

    private ContainerService containerService;

    private InsulinContainer newContainer(BigDecimal initialUnits, ContainerStatus status) {
        InsulinContainer container = new InsulinContainer();
        container.setId(1L);
        container.setInitialUnits(initialUnits);
        container.setStatus(status);
        return container;
    }

    private void setUp() {
        containerService = new ContainerService(containerRepository, stockMovementRepository, entityManager);
    }

    @Test
    void discardedContainerNeverChangesStatusRegardlessOfBalance() {
        setUp();
        InsulinContainer container = newContainer(new BigDecimal("100.00"), ContainerStatus.DISCARDED);

        containerService.syncContainerStatus(container);

        assertThat(container.getStatus()).isEqualTo(ContainerStatus.DISCARDED);
    }

    @Test
    void zeroOrNegativeBalanceBecomesEmpty() {
        setUp();
        InsulinContainer container = newContainer(new BigDecimal("100.00"), ContainerStatus.OPEN);
        when(stockMovementRepository.sumQuantityByContainerId(1L)).thenReturn(BigDecimal.ZERO);

        containerService.syncContainerStatus(container);

        assertThat(container.getStatus()).isEqualTo(ContainerStatus.EMPTY);
    }

    @Test
    void balanceEqualToInitialUnitsResealsAndClearsOpenedAt() {
        setUp();
        InsulinContainer container = newContainer(new BigDecimal("100.00"), ContainerStatus.OPEN);
        container.setOpenedAt(java.time.Instant.now());
        when(stockMovementRepository.sumQuantityByContainerId(1L)).thenReturn(new BigDecimal("100.00"));

        containerService.syncContainerStatus(container);

        assertThat(container.getStatus()).isEqualTo(ContainerStatus.SEALED);
        assertThat(container.getOpenedAt()).isNull();
    }

    @Test
    void partialBalanceIsOpen() {
        setUp();
        InsulinContainer container = newContainer(new BigDecimal("100.00"), ContainerStatus.SEALED);
        when(stockMovementRepository.sumQuantityByContainerId(1L)).thenReturn(new BigDecimal("40.00"));

        containerService.syncContainerStatus(container);

        assertThat(container.getStatus()).isEqualTo(ContainerStatus.OPEN);
    }

    @Test
    void fifoOrderPutsOpenContainersBeforeSealedOnes() {
        setUp();
        InsulinContainer sealed = newContainer(new BigDecimal("100.00"), ContainerStatus.SEALED);
        sealed.setId(1L);
        InsulinContainer open = newContainer(new BigDecimal("100.00"), ContainerStatus.OPEN);
        open.setId(2L);

        when(containerRepository.findByInsulinIdOrderByCreatedAtAscIdAsc(10L))
                .thenReturn(java.util.List.of(sealed, open));

        var fifo = containerService.getAvailableContainersFifo(10L);

        assertThat(fifo).containsExactly(open, sealed);
    }
}
