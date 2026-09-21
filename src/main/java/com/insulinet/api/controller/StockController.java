package com.insulinet.api.controller;

import com.insulinet.api.mapper.StockMapper;
import com.insulinet.api.model.dto.stock.InsulinContainerResponse;
import com.insulinet.api.model.dto.stock.StockAdjustmentRequest;
import com.insulinet.api.model.dto.stock.StockInRequest;
import com.insulinet.api.model.dto.stock.StockInUpdateRequest;
import com.insulinet.api.model.dto.stock.StockMovementResponse;
import com.insulinet.api.model.dto.stock.StockSummaryResponse;
import com.insulinet.api.model.entity.Insulin;
import com.insulinet.api.model.entity.InsulinContainer;
import com.insulinet.api.model.entity.User;
import com.insulinet.api.service.ContainerService;
import com.insulinet.api.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/insulins")
public class StockController {

    private final StockService stockService;
    private final ContainerService containerService;
    private final StockMapper stockMapper;

    public StockController(StockService stockService, ContainerService containerService, StockMapper stockMapper) {
        this.stockService = stockService;
        this.containerService = containerService;
        this.stockMapper = stockMapper;
    }

    @PostMapping("/{insulinId}/stock")
    @ResponseStatus(HttpStatus.CREATED)
    public List<StockMovementResponse> addStock(
            @PathVariable Long insulinId,
            @Valid @RequestBody StockInRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return stockService.addStock(currentUser, insulinId, request.containers()).stream()
                .map(stockMapper::toResponse)
                .toList();
    }

    @GetMapping("/{insulinId}/stock")
    public StockSummaryResponse getStock(
            @PathVariable Long insulinId,
            @AuthenticationPrincipal User currentUser
    ) {
        Insulin insulin = stockService.getInsulinWithActiveCheck(currentUser, insulinId);
        BigDecimal currentStock = stockService.calculateCurrentStock(insulin.getId());
        return new StockSummaryResponse(insulin.getId(), insulin.getName(), currentStock);
    }

    @GetMapping("/{insulinId}/containers")
    public List<InsulinContainerResponse> getContainers(
            @PathVariable Long insulinId,
            @AuthenticationPrincipal User currentUser
    ) {
        return stockService.getContainers(currentUser, insulinId).stream()
                .map(container -> stockMapper.toResponse(
                        container, containerService.calculateContainerRemaining(container.getId())))
                .toList();
    }

    @PostMapping("/{insulinId}/containers/{containerId}/discard")
    public InsulinContainerResponse discardContainer(
            @PathVariable Long insulinId,
            @PathVariable Long containerId,
            @AuthenticationPrincipal User currentUser
    ) {
        InsulinContainer container = stockService.discardContainer(currentUser, insulinId, containerId);
        return stockMapper.toResponse(container, containerService.calculateContainerRemaining(container.getId()));
    }

    @PostMapping("/{insulinId}/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public List<StockMovementResponse> adjustStock(
            @PathVariable Long insulinId,
            @Valid @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return stockService.adjustStock(currentUser, insulinId, request.actualStockUnits(), request.notes())
                .stream()
                .map(stockMapper::toResponse)
                .toList();
    }

    @PatchMapping("/{insulinId}/stock/{movementId}")
    public StockMovementResponse updateStockEntry(
            @PathVariable Long insulinId,
            @PathVariable Long movementId,
            @Valid @RequestBody StockInUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return stockMapper.toResponse(
                stockService.updateStockEntry(currentUser, insulinId, movementId, request.units()));
    }
}
