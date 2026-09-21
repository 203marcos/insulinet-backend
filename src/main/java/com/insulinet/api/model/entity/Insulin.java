package com.insulinet.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "insulin")
public class Insulin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "concentration_units_per_ml", nullable = false, precision = 10, scale = 2)
    private BigDecimal concentrationUnitsPerMl;

    @Column(name = "container_volume_ml", nullable = false, precision = 10, scale = 2)
    private BigDecimal containerVolumeMl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getConcentrationUnitsPerMl() {
        return concentrationUnitsPerMl;
    }

    public void setConcentrationUnitsPerMl(BigDecimal concentrationUnitsPerMl) {
        this.concentrationUnitsPerMl = concentrationUnitsPerMl;
    }

    public BigDecimal getContainerVolumeMl() {
        return containerVolumeMl;
    }

    public void setContainerVolumeMl(BigDecimal containerVolumeMl) {
        this.containerVolumeMl = containerVolumeMl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
