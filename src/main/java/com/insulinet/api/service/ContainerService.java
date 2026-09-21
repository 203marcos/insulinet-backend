package com.insulinet.api.service;

import com.insulinet.api.model.entity.InsulinContainer;
import com.insulinet.api.model.enums.ContainerStatus;
import com.insulinet.api.repository.InsulinContainerRepository;
import com.insulinet.api.repository.StockMovementRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Espelha app/services/container_service.py: calculo de saldo por container
 * e as transicoes de status derivadas desse saldo (nunca ha um campo de
 * saldo persistido).
 */
@Service
public class ContainerService {

    private final InsulinContainerRepository containerRepository;
    private final StockMovementRepository stockMovementRepository;
    private final EntityManager entityManager;

    public ContainerService(
            InsulinContainerRepository containerRepository,
            StockMovementRepository stockMovementRepository,
            EntityManager entityManager
    ) {
        this.containerRepository = containerRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.entityManager = entityManager;
    }

    public List<InsulinContainer> listContainers(Long insulinId) {
        return containerRepository.findByInsulinIdOrderByCreatedAtAscIdAsc(insulinId);
    }

    public BigDecimal calculateContainerRemaining(Long containerId) {
        return stockMovementRepository.sumQuantityByContainerId(containerId);
    }

    public void syncContainerStatus(InsulinContainer container) {
        if (container.getStatus() == ContainerStatus.DISCARDED) {
            return;
        }

        BigDecimal remaining = calculateContainerRemaining(container.getId());

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            container.setStatus(ContainerStatus.EMPTY);
            return;
        }

        if (remaining.compareTo(container.getInitialUnits()) == 0) {
            container.setStatus(ContainerStatus.SEALED);
            container.setOpenedAt(null);
            return;
        }

        container.setStatus(ContainerStatus.OPEN);
        entityManager.flush();
    }

    public void openContainer(InsulinContainer container) {
        if (container.getStatus() == ContainerStatus.SEALED) {
            container.setStatus(ContainerStatus.OPEN);
            container.setOpenedAt(Instant.now());
            entityManager.flush();
        }
    }

    public List<InsulinContainer> getAvailableContainersFifo(Long insulinId) {
        List<InsulinContainer> containers = listContainers(insulinId);

        List<InsulinContainer> fifoOrder = new ArrayList<>(containers.stream()
                .filter(c -> c.getStatus() == ContainerStatus.OPEN)
                .toList());
        fifoOrder.addAll(containers.stream()
                .filter(c -> c.getStatus() == ContainerStatus.SEALED)
                .toList());

        return fifoOrder;
    }
}
