package com.insulinet.api.controller;

import com.insulinet.api.mapper.InsulinMapper;
import com.insulinet.api.model.dto.insulin.InsulinCreateRequest;
import com.insulinet.api.model.dto.insulin.InsulinResponse;
import com.insulinet.api.model.dto.insulin.InsulinSummaryResponse;
import com.insulinet.api.model.dto.insulin.InsulinUpdateRequest;
import com.insulinet.api.model.dto.insulin.StockHistoryItemResponse;
import com.insulinet.api.model.entity.Insulin;
import com.insulinet.api.model.entity.User;
import com.insulinet.api.service.InsulinService;
import com.insulinet.api.service.ProjectionService;
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

import java.util.List;

@RestController
@RequestMapping("/api/insulins")
public class InsulinController {

    private final InsulinService insulinService;
    private final ProjectionService projectionService;
    private final InsulinMapper insulinMapper;

    public InsulinController(
            InsulinService insulinService,
            ProjectionService projectionService,
            InsulinMapper insulinMapper
    ) {
        this.insulinService = insulinService;
        this.projectionService = projectionService;
        this.insulinMapper = insulinMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InsulinResponse create(
            @Valid @RequestBody InsulinCreateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        Insulin insulin = insulinService.create(currentUser, request);
        return insulinMapper.toResponse(insulin);
    }

    @GetMapping
    public List<InsulinResponse> list(@AuthenticationPrincipal User currentUser) {
        return insulinService.list(currentUser).stream().map(insulinMapper::toResponse).toList();
    }

    @GetMapping("/{insulinId}/history")
    public List<StockHistoryItemResponse> history(
            @PathVariable Long insulinId,
            @AuthenticationPrincipal User currentUser
    ) {
        return insulinService.getHistory(currentUser, insulinId).stream()
                .map(insulinMapper::toHistoryItem)
                .toList();
    }

    @GetMapping("/{insulinId}/summary")
    public InsulinSummaryResponse summary(
            @PathVariable Long insulinId,
            @AuthenticationPrincipal User currentUser
    ) {
        Insulin insulin = insulinService.getOwned(currentUser, insulinId);
        return projectionService.buildInsulinSummary(insulin);
    }

    @PatchMapping("/{insulinId}")
    public InsulinResponse update(
            @PathVariable Long insulinId,
            @Valid @RequestBody InsulinUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        Insulin insulin = insulinService.update(currentUser, insulinId, request);
        return insulinMapper.toResponse(insulin);
    }
}
