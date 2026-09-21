package com.insulinet.api.controller;

import com.insulinet.api.mapper.StockMapper;
import com.insulinet.api.model.dto.dose.DoseBatchCreateRequest;
import com.insulinet.api.model.dto.dose.DoseCreateRequest;
import com.insulinet.api.model.dto.dose.DoseUpdateRequest;
import com.insulinet.api.model.dto.stock.StockMovementResponse;
import com.insulinet.api.model.entity.User;
import com.insulinet.api.service.DoseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/insulins")
public class DoseController {

    private final DoseService doseService;
    private final StockMapper stockMapper;

    public DoseController(DoseService doseService, StockMapper stockMapper) {
        this.doseService = doseService;
        this.stockMapper = stockMapper;
    }

    @PostMapping("/{insulinId}/doses")
    @ResponseStatus(HttpStatus.CREATED)
    public List<StockMovementResponse> registerDose(
            @PathVariable Long insulinId,
            @Valid @RequestBody DoseCreateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return doseService.registerDose(currentUser, insulinId, request).stream()
                .map(stockMapper::toResponse)
                .toList();
    }

    @PatchMapping("/{insulinId}/doses/{doseId}")
    public List<StockMovementResponse> updateDose(
            @PathVariable Long insulinId,
            @PathVariable Long doseId,
            @Valid @RequestBody DoseUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return doseService.updateDose(currentUser, insulinId, doseId, request).stream()
                .map(stockMapper::toResponse)
                .toList();
    }

    @DeleteMapping("/{insulinId}/doses/{doseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDose(
            @PathVariable Long insulinId,
            @PathVariable Long doseId,
            @AuthenticationPrincipal User currentUser
    ) {
        doseService.deleteDose(currentUser, insulinId, doseId);
    }

    @PostMapping("/{insulinId}/dose-batches")
    @ResponseStatus(HttpStatus.CREATED)
    public List<StockMovementResponse> registerDoseBatch(
            @PathVariable Long insulinId,
            @Valid @RequestBody DoseBatchCreateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return doseService.registerDoseBatch(currentUser, insulinId, request).stream()
                .map(stockMapper::toResponse)
                .toList();
    }
}
