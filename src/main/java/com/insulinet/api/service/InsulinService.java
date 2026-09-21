package com.insulinet.api.service;

import com.insulinet.api.exception.BadRequestException;
import com.insulinet.api.exception.ConflictException;
import com.insulinet.api.exception.ResourceNotFoundException;
import com.insulinet.api.model.dto.insulin.InsulinCreateRequest;
import com.insulinet.api.model.dto.insulin.InsulinUpdateRequest;
import com.insulinet.api.model.entity.Insulin;
import com.insulinet.api.model.entity.StockMovement;
import com.insulinet.api.model.entity.User;
import com.insulinet.api.repository.InsulinRepository;
import com.insulinet.api.repository.StockMovementRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Espelha app/services/insulin_service.py (posse e status ativo/inativo) e
 * as rotas de CRUD de app/api/routes/insulins.py, que no Python continham
 * regra de negocio diretamente no controller.
 */
@Service
public class InsulinService {

    private final InsulinRepository insulinRepository;
    private final StockMovementRepository stockMovementRepository;
    private final EntityManager entityManager;

    public InsulinService(
            InsulinRepository insulinRepository,
            StockMovementRepository stockMovementRepository,
            EntityManager entityManager
    ) {
        this.insulinRepository = insulinRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.entityManager = entityManager;
    }

    public Insulin getOwned(User user, Long insulinId) {
        return insulinRepository.findByIdAndUserId(insulinId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Insulina nao encontrada."));
    }

    public void ensureActive(Insulin insulin) {
        if (!insulin.isActive()) {
            throw new ConflictException(
                    "Esta insulina esta inativa. Ative-a novamente para registrar novas movimentacoes.");
        }
    }

    @Transactional
    public Insulin create(User user, InsulinCreateRequest request) {
        Insulin insulin = new Insulin();
        insulin.setUser(user);
        insulin.setName(request.name());
        insulin.setConcentrationUnitsPerMl(request.concentrationUnitsPerMl());
        insulin.setContainerVolumeMl(request.containerVolumeMl());
        insulin.setActive(true);
        insulin.setCreatedAt(Instant.now());
        insulinRepository.save(insulin);
        // recarrega do banco para refletir a escala exata do NUMERIC(10,2)
        // (ex.: cliente envia "100" -> Postgres guarda/retorna "100.00");
        // mesmo padrao do db.refresh() usado em todo o backend Python.
        entityManager.refresh(insulin);
        return insulin;
    }

    public List<Insulin> list(User user) {
        return insulinRepository.findByUserIdOrderById(user.getId());
    }

    @Transactional
    public Insulin update(User user, Long insulinId, InsulinUpdateRequest request) {
        Insulin insulin = getOwned(user, insulinId);
        String cleanName = request.name().strip();

        if (cleanName.isEmpty()) {
            throw new BadRequestException("O nome da insulina nao pode ficar vazio.");
        }

        insulin.setName(cleanName);
        insulin.setConcentrationUnitsPerMl(request.concentrationUnitsPerMl());
        insulin.setContainerVolumeMl(request.containerVolumeMl());
        insulin.setActive(request.active());
        entityManager.flush();
        entityManager.refresh(insulin);
        return insulin;
    }

    public List<StockMovement> getHistory(User user, Long insulinId) {
        Insulin insulin = getOwned(user, insulinId);
        return stockMovementRepository.findByInsulinIdOrderByOccurredAtDescIdDesc(insulin.getId());
    }
}
