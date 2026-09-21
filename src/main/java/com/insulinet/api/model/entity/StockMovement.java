package com.insulinet.api.model.entity;

import com.insulinet.api.model.enums.MovementType;
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
@Table(name = "stock_movement")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insulin_id", nullable = false)
    private Insulin insulin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false)
    private InsulinContainer container;

    /**
     * FK auto-referenciada para agrupar movimentos que resultam de uma unica
     * operacao (dose/ajuste) distribuida por mais de um container. Mapeado
     * como Long simples (nao como relacionamento) pois so precisamos ler e
     * escrever o id, nunca navegar o grafo de objetos.
     */
    @Column(name = "group_id")
    private Long groupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    @Column(name = "quantity_units", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityUnits;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "occurred_time_known", nullable = false)
    private boolean occurredTimeKnown = true;

    @Column(length = 500)
    private String notes;

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

    public InsulinContainer getContainer() {
        return container;
    }

    public void setContainer(InsulinContainer container) {
        this.container = container;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public BigDecimal getQuantityUnits() {
        return quantityUnits;
    }

    public void setQuantityUnits(BigDecimal quantityUnits) {
        this.quantityUnits = quantityUnits;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public boolean isOccurredTimeKnown() {
        return occurredTimeKnown;
    }

    public void setOccurredTimeKnown(boolean occurredTimeKnown) {
        this.occurredTimeKnown = occurredTimeKnown;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
