package com.insulinet.api.service;

import com.insulinet.api.exception.BadRequestException;
import com.insulinet.api.exception.ConflictException;
import com.insulinet.api.exception.InsufficientStockException;
import com.insulinet.api.exception.ResourceNotFoundException;
import com.insulinet.api.model.dto.dose.DoseBatchCreateRequest;
import com.insulinet.api.model.dto.dose.DoseBatchItemRequest;
import com.insulinet.api.model.dto.dose.DoseCreateRequest;
import com.insulinet.api.model.dto.dose.DoseUpdateRequest;
import com.insulinet.api.model.entity.Insulin;
import com.insulinet.api.model.entity.InsulinContainer;
import com.insulinet.api.model.entity.StockMovement;
import com.insulinet.api.model.entity.User;
import com.insulinet.api.model.enums.MovementType;
import com.insulinet.api.repository.StockMovementRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Espelha app/services/dose_service.py (resolucao de data/hora local) e a
 * orquestracao de app/api/routes/doses.py.
 *
 * Diferenca deliberada em relacao ao Python: aqui TODAS as operacoes que
 * criam/alteram estoque de uma insulina (incluindo doses) exigem que ela
 * esteja ativa. No backend original, ensure_insulin_active() era chamado em
 * stock.py mas nao em doses.py - uma inconsistencia identificada na analise
 * e corrigida nesta migracao, com aprovacao do time.
 */
@Service
public class DoseService {

    private final StockMovementRepository stockMovementRepository;
    private final StockService stockService;
    private final ContainerService containerService;
    private final InsulinService insulinService;
    private final ZoneId appTimeZone;
    private final EntityManager entityManager;

    public DoseService(
            StockMovementRepository stockMovementRepository,
            StockService stockService,
            ContainerService containerService,
            InsulinService insulinService,
            ZoneId appTimeZone,
            EntityManager entityManager
    ) {
        this.stockMovementRepository = stockMovementRepository;
        this.stockService = stockService;
        this.containerService = containerService;
        this.insulinService = insulinService;
        this.appTimeZone = appTimeZone;
        this.entityManager = entityManager;
    }

    public record ResolvedDoseTime(Instant occurredAt, boolean timeKnown) {
    }

    public ResolvedDoseTime resolveDoseDateTime(LocalDate occurredDate, LocalTime occurredTime) {
        ZonedDateTime nowLocal = ZonedDateTime.now(appTimeZone);

        if (occurredDate == null) {
            return new ResolvedDoseTime(nowLocal.toInstant(), true);
        }

        if (occurredDate.isAfter(nowLocal.toLocalDate())) {
            throw new BadRequestException("A data da aplicacao nao pode estar no futuro.");
        }

        if (occurredTime == null) {
            ZonedDateTime localDateTime;
            if (occurredDate.isEqual(nowLocal.toLocalDate())) {
                localDateTime = nowLocal.withSecond(0).withNano(0);
            } else {
                localDateTime = occurredDate.atTime(LocalTime.NOON).atZone(appTimeZone);
            }
            return new ResolvedDoseTime(localDateTime.toInstant(), false);
        }

        ZonedDateTime localDateTime = occurredDate.atTime(occurredTime).atZone(appTimeZone);

        if (localDateTime.isAfter(nowLocal)) {
            throw new BadRequestException("A data e hora da aplicacao nao podem estar no futuro.");
        }

        return new ResolvedDoseTime(localDateTime.toInstant(), true);
    }

    @Transactional
    public List<StockMovement> registerDose(User user, Long insulinId, DoseCreateRequest request) {
        Insulin insulin = insulinService.getOwned(user, insulinId);
        insulinService.ensureActive(insulin);

        BigDecimal currentStock = stockService.calculateCurrentStock(insulin.getId());

        if (request.units().compareTo(currentStock) > 0) {
            throw new ConflictException("Estoque insuficiente. Estoque atual: " + currentStock + " U");
        }

        ResolvedDoseTime resolved = resolveDoseDateTime(request.occurredDate(), request.occurredTime());

        try {
            return stockService.distributeNegativeDelta(
                    insulin, request.units(), MovementType.DOSE,
                    resolved.occurredAt(), resolved.timeKnown(), request.notes());
        } catch (InsufficientStockException e) {
            throw new ConflictException("Estoque insuficiente entre as canetas/frascos disponiveis.");
        }
    }

    @Transactional
    public List<StockMovement> updateDose(User user, Long insulinId, Long doseId, DoseUpdateRequest request) {
        Insulin insulin = insulinService.getOwned(user, insulinId);
        insulinService.ensureActive(insulin);

        List<StockMovement> groupMovements = getDoseGroup(insulin.getId(), doseId);

        BigDecimal oldUnitsTotal = groupMovements.stream()
                .map(m -> m.getQuantityUnits().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentStock = stockService.calculateCurrentStock(insulin.getId());
        BigDecimal availableUnits = currentStock.add(oldUnitsTotal);

        if (request.units().compareTo(availableUnits) > 0) {
            throw new ConflictException(
                    "Estoque insuficiente para essa alteracao. Maximo disponivel: " + availableUnits + " U");
        }

        ResolvedDoseTime resolved = resolveDoseDateTime(request.occurredDate(), request.occurredTime());

        Map<Long, InsulinContainer> affectedContainers = deleteDoseGroup(groupMovements);
        for (InsulinContainer container : affectedContainers.values()) {
            containerService.syncContainerStatus(container);
        }

        try {
            return stockService.distributeNegativeDelta(
                    insulin, request.units(), MovementType.DOSE,
                    resolved.occurredAt(), resolved.timeKnown(), request.notes());
        } catch (InsufficientStockException e) {
            throw new ConflictException("Estoque insuficiente entre as canetas/frascos disponiveis.");
        }
    }

    @Transactional
    public void deleteDose(User user, Long insulinId, Long doseId) {
        Insulin insulin = insulinService.getOwned(user, insulinId);

        List<StockMovement> groupMovements = getDoseGroup(insulin.getId(), doseId);
        Map<Long, InsulinContainer> affectedContainers = deleteDoseGroup(groupMovements);

        for (InsulinContainer container : affectedContainers.values()) {
            containerService.syncContainerStatus(container);
        }
    }

    @Transactional
    public List<StockMovement> registerDoseBatch(User user, Long insulinId, DoseBatchCreateRequest request) {
        Insulin insulin = insulinService.getOwned(user, insulinId);
        insulinService.ensureActive(insulin);

        BigDecimal currentStock = stockService.calculateCurrentStock(insulin.getId());
        BigDecimal totalUnits = request.doses().stream()
                .map(DoseBatchItemRequest::units)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalUnits.compareTo(currentStock) > 0) {
            throw new ConflictException(
                    "Estoque insuficiente para registrar todas as aplicacoes. Estoque atual: "
                            + currentStock + " U. Total informado: " + totalUnits + " U.");
        }

        List<StockMovement> allMovements = new java.util.ArrayList<>();

        for (DoseBatchItemRequest item : request.doses()) {
            ResolvedDoseTime resolved = resolveDoseDateTime(item.occurredDate(), item.occurredTime());

            try {
                allMovements.addAll(stockService.distributeNegativeDelta(
                        insulin, item.units(), MovementType.DOSE,
                        resolved.occurredAt(), resolved.timeKnown(), item.notes()));
            } catch (InsufficientStockException e) {
                throw new ConflictException("Estoque insuficiente para registrar todas as aplicacoes.");
            }
        }

        return allMovements;
    }

    private List<StockMovement> getDoseGroup(Long insulinId, Long doseId) {
        StockMovement anchor = stockMovementRepository
                .findByIdAndInsulinIdAndMovementType(doseId, insulinId, MovementType.DOSE)
                .orElseThrow(() -> new ResourceNotFoundException("Aplicacao nao encontrada."));

        Long groupKey = anchor.getGroupId() != null ? anchor.getGroupId() : anchor.getId();

        return stockMovementRepository.findGroup(insulinId, MovementType.DOSE, groupKey);
    }

    /**
     * group_id e uma FK auto-referenciada nesta tabela. Um unico flush()
     * agrupa deletes da mesma tabela independentemente da ordem de chamada,
     * entao removemos os "filhos" (group_id preenchido) num flush separado
     * antes de remover as "ancoras" que eles referenciam, evitando violar a
     * FK - mesma ordem usada no backend Python original.
     */
    private Map<Long, InsulinContainer> deleteDoseGroup(List<StockMovement> groupMovements) {
        Map<Long, InsulinContainer> affectedContainers = new LinkedHashMap<>();
        for (StockMovement movement : groupMovements) {
            affectedContainers.put(movement.getContainer().getId(), movement.getContainer());
        }

        List<StockMovement> children = groupMovements.stream()
                .filter(m -> m.getGroupId() != null)
                .toList();
        List<StockMovement> anchors = groupMovements.stream()
                .filter(m -> m.getGroupId() == null)
                .toList();

        children.forEach(stockMovementRepository::delete);
        entityManager.flush();

        anchors.forEach(stockMovementRepository::delete);
        entityManager.flush();

        return affectedContainers;
    }
}
