CREATE TABLE insulin (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    concentration_units_per_ml NUMERIC(10, 2) NOT NULL,
    container_volume_ml NUMERIC(10, 2) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

-- movement_type e modelado como VARCHAR + CHECK (em vez de ENUM nativo do
-- Postgres) para ter mapeamento @Enumerated(EnumType.STRING) simples e
-- portavel no Hibernate, sem depender de binding JDBC especifico de enum.
CREATE TABLE stock_movement (
    id BIGSERIAL PRIMARY KEY,
    insulin_id BIGINT NOT NULL REFERENCES insulin (id),
    movement_type VARCHAR(20) NOT NULL
        CHECK (movement_type IN ('STOCK_IN', 'DOSE', 'DISCARD', 'ADJUSTMENT')),
    quantity_units NUMERIC(10, 2) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL
);
