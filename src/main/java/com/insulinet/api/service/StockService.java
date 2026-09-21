package com.insulinet.api.service;

import com.insulinet.api.exception.BadRequestException;
import com.insulinet.api.exception.ConflictException;
import com.insulinet.api.exception.InsufficientStockException;
import com.insulinet.api.exception.NoContainerAvailableException;
import com.insulinet.api.exception.ResourceNotFoundException;
import com.insulinet.api.model.entity.Insulin;
import com.insulinet.api.model.entity.InsulinContainer;
import com.insulinet.api.model.entity.StockMovement;
import com.insulinet.api.model.entity.User;
import com.insulinet.api.model.enums.ContainerStatus;
import com.insulinet.api.model.enums.MovementType;
import com.insulinet.api.repository.InsulinContainerRepository;
import com.insulinet.api.repository.StockMovementRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Espelha app/services/stock_service.py (distribuicao FIFO entre containers)
 * e a orquestracao que no Python vivia diretamente em
 * app/api/routes/stock.py.
 */
@Service
public class StockService {

    private final InsulinContainerRepository containerRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ContainerService containerService;
    private final InsulinService insulinService;
    private final EntityManager entityManager;

    public StockService(
            InsulinContainerRepository containerRepository,
            StockMovementRepository stockMovementRepository,
            ContainerService containerService,
            InsulinService insulinService,
            EntityManager entityManager
    ) {
        this.containerRepository = containerRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.containerService = containerService;
        this.insulinService = insulinService;
        this.entityManager = entityManager;
    }

    public BigDecimal calculateCurrentStock(Long insulinId) {
        return stockMovementRepository.sumQuantityByInsulinId(insulinId);
    }

    @Transactional
    public List<StockMovement> createStockInContainers(Insulin insulin, int containersCount) {
        BigDecimal unitsPerContainer = insulin.getConcentrationUnitsPerMl()
                .multiply(insulin.getContainerVolumeMl());

        List<StockMovement> movements = new ArrayList<>();

        for (int i = 0; i < containersCount; i++) {
            InsulinContainer container = new InsulinContainer();
            container.setInsulin(insulin);
            container.setInitialUnits(unitsPerContainer);
            container.setStatus(ContainerStatus.SEALED);
            container.setCreatedAt(Instant.now());
            containerRepository.save(container);
            entityManager.flush();

            StockMovement movement = new StockMovement();
            movement.setInsulin(insulin);
            movement.setContainer(container);
            movement.setMovementType(MovementType.STOCK_IN);
            movement.setQuantityUnits(unitsPerContainer);
            movement.setOccurredAt(Instant.now());
            movement.setOccurredTimeKnown(true);
            movement.setNotes("Entrada de 1 recipiente");
            movement.setCreatedAt(Instant.now());
            stockMovementRepository.save(movement);
            entityManager.flush();
            // recarrega para refletir a escala exata do NUMERIC(10,2): a
            // multiplicacao de dois BigDecimal com escala 2 produz escala 4
            // (ex.: 100.00 * 3.00 = 300.0000); o Postgres normaliza para 2
            // casas ao gravar, e o refresh traz esse valor normalizado de
            // volta - mesmo padrao do db.refresh() no backend Python.
            entityManager.refresh(movement);

            movements.add(movement);
        }

        return movements;
    }

    @Transactional
    public List<StockMovement> distributeNegativeDelta(
            Insulin insulin,
            BigDecimal units,
            MovementType movementType,
            Instant occurredAt,
            boolean occurredTimeKnown,
            String notes
    ) {
        BigDecimal remainingToDeduct = units;
        List<StockMovement> movements = new ArrayList<>();

        for (InsulinContainer container : containerService.getAvailableContainersFifo(insulin.getId())) {
            if (remainingToDeduct.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal containerRemaining = containerService.calculateContainerRemaining(container.getId());

            if (containerRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                containerService.syncContainerStatus(container);
                continue;
            }

            containerService.openContainer(container);

            BigDecimal take = remainingToDeduct.min(containerRemaining);

            StockMovement movement = new StockMovement();
            movement.setInsulin(insulin);
            movement.setContainer(container);
            movement.setMovementType(movementType);
            movement.setQuantityUnits(take.negate());
            movement.setOccurredAt(occurredAt);
            movement.setOccurredTimeKnown(occurredTimeKnown);
            movement.setNotes(notes);
            movement.setCreatedAt(Instant.now());
            stockMovementRepository.save(movement);
            entityManager.flush();
            entityManager.refresh(movement);
            movements.add(movement);

            remainingToDeduct = remainingToDeduct.subtract(take);
            containerService.syncContainerStatus(container);
        }

        if (remainingToDeduct.compareTo(BigDecimal.ZERO) > 0) {
            throw new InsufficientStockException(remainingToDeduct);
        }

        if (movements.size() > 1) {
            Long anchorId = movements.get(0).getId();
            for (int i = 1; i < movements.size(); i++) {
                movements.get(i).setGroupId(anchorId);
            }
            entityManager.flush();
        }

        return movements;
    }

    @Transactional
    public StockMovement applyPositiveAdjustment(
            Insulin insulin,
            BigDecimal units,
            Instant occurredAt,
            String notes
    ) {
        List<InsulinContainer> containers = containerService.getAvailableContainersFifo(insulin.getId());

        if (containers.isEmpty()) {
            throw new NoContainerAvailableException();
        }

        InsulinContainer container = containers.get(0);
        containerService.openContainer(container);

        StockMovement movement = new StockMovement();
        movement.setInsulin(insulin);
        movement.setContainer(container);
        movement.setMovementType(MovementType.ADJUSTMENT);
        movement.setQuantityUnits(units);
        movement.setOccurredAt(occurredAt);
        movement.setOccurredTimeKnown(true);
        movement.setNotes(notes);
        movement.setCreatedAt(Instant.now());
        stockMovementRepository.save(movement);
        entityManager.flush();
        entityManager.refresh(movement);

        containerService.syncContainerStatus(container);

        return movement;
    }

    // --- casos de uso expostos aos controllers (equivalente a stock.py) ---

    @Transactional
    public List<StockMovement> addStock(User user, Long insulinId, int containersCount) {
        Insulin insulin = insulinService.getOwned(user, insulinId);
        insulinService.ensureActive(insulin);
        return createStockInContainers(insulin, containersCount);
    }

    /**
     * Retorna a insulina (ja com posse/ativo validados) junto do saldo atual,
     * para montar o StockSummaryResponse no controller.
     */
    public Insulin getInsulinWithActiveCheck(User user, Long insulinId) {
        Insulin insulin = insulinService.getOwned(user, insulinId);
        insulinService.ensureActive(insulin);
        return insulin;
    }

    public List<InsulinContainer> getContainers(User user, Long insulinId) {
        Insulin insulin = insulinService.getOwned(user, insulinId);
        return containerService.listContainers(insulin.getId());
    }

    @Transactional
    public InsulinContainer discardContainer(User user, Long insulinId, Long containerId) {
        Insulin insulin = insulinService.getOwned(user, insulinId);
        insulinService.ensureActive(insulin);

        InsulinContainer container = containerRepository.findByIdAndInsulinId(containerId, insulin.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Caneta/frasco nao encontrado."));

        if (container.getStatus() == ContainerStatus.DISCARDED) {
            throw new ConflictException("Essa caneta/frasco ja foi descartado.");
        }

        BigDecimal remaining = containerService.calculateContainerRemaining(container.getId());

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            StockMovement movement = new StockMovement();
            movement.setInsulin(insulin);
            movement.setContainer(container);
            movement.setMovementType(MovementType.DISCARD);
            movement.setQuantityUnits(remaining.negate());
            movement.setOccurredAt(Instant.now());
            movement.setOccurredTimeKnown(true);
            movement.setNotes("Caneta/frasco descartado manualmente.");
            movement.setCreatedAt(Instant.now());
            stockMovementRepository.save(movement);
            entityManager.flush();
        }

        container.setStatus(ContainerStatus.DISCARDED);

        return container;
    }

    @Transactional
    public List<StockMovement> adjustStock(User user, Long insulinId, BigDecimal actualStockUnits, String rawNotes) {
        Insulin insulin = insulinService.getOwned(user, insulinId);
        insulinService.ensureActive(insulin);

        BigDecimal currentStock = calculateCurrentStock(insulin.getId());
        BigDecimal difference = actualStockUnits.subtract(currentStock);

        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException(
                    "O estoque informado ja e igual ao estoque calculado pelo sistema.");
        }

        Instant occurredAt = Instant.now();
        String notes = rawNotes.strip();

        try {
            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                return List.of(applyPositiveAdjustment(insulin, difference, occurredAt, notes));
            }
            return distributeNegativeDelta(
                    insulin, difference.negate(), MovementType.ADJUSTMENT, occurredAt, true, notes);
        } catch (InsufficientStockException e) {
            throw new ConflictException(
                    "Nao ha estoque suficiente distribuido entre as canetas/frascos para aplicar esse ajuste.");
        } catch (NoContainerAvailableException e) {
            throw new ConflictException(
                    "Nenhuma caneta/frasco em estoque para ajustar. Adicione estoque primeiro.");
        }
    }

    @Transactional
    public StockMovement updateStockEntry(User user, Long insulinId, Long movementId, BigDecimal newUnits) {
        Insulin insulin = insulinService.getOwned(user, insulinId);

        StockMovement movement = stockMovementRepository
                .findByIdAndInsulinIdAndMovementType(movementId, insulin.getId(), MovementType.STOCK_IN)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada de estoque nao encontrada."));

        InsulinContainer container = movement.getContainer();

        if (container.getStatus() != ContainerStatus.SEALED) {
            throw new ConflictException(
                    "Nao e possivel editar essa entrada: a caneta/frasco ja foi aberto ou utilizado.");
        }

        container.setInitialUnits(newUnits);
        movement.setQuantityUnits(newUnits);

        entityManager.flush();
        entityManager.refresh(movement);

        return movement;
    }
}
