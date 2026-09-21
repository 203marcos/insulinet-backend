package com.insulinet.api.model.entity;

import com.insulinet.api.model.enums.ContainerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "insulin_container")
public class InsulinContainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insulin_id", nullable = false)
    private Insulin insulin;

    @Column(name = "initial_units", nullable = false, precision = 10, scale = 2)
    private BigDecimal initialUnits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContainerStatus status = ContainerStatus.SEALED;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Insulin getInsulin() {
        return insulin;
    }

    public void setInsulin(Insulin insulin) {
        this.insulin = insulin;
    }

    public BigDecimal getInitialUnits() {
        return initialUnits;
    }

    public void setInitialUnits(BigDecimal initialUnits) {
        this.initialUnits = initialUnits;
    }

    public ContainerStatus getStatus() {
        return status;
    }

    public void setStatus(ContainerStatus status) {
        this.status = status;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
