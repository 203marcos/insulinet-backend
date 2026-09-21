CREATE TABLE insulin_container (
    id BIGSERIAL PRIMARY KEY,
    insulin_id BIGINT NOT NULL REFERENCES insulin (id),
    initial_units NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('SEALED', 'OPEN', 'EMPTY', 'DISCARDED')),
    opened_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE stock_movement ADD COLUMN container_id BIGINT;
ALTER TABLE stock_movement ADD COLUMN group_id BIGINT;

ALTER TABLE stock_movement
    ADD CONSTRAINT stock_movement_container_id_fkey
    FOREIGN KEY (container_id) REFERENCES insulin_container (id);

ALTER TABLE stock_movement
    ADD CONSTRAINT stock_movement_group_id_fkey
    FOREIGN KEY (group_id) REFERENCES stock_movement (id);

-- Backfill: cria um container legado por insulina existente e vincula os
-- movimentos ja registrados a ele, replicando a migration equivalente do
-- Alembic (a8f1c6d92e3b_add_insulin_container_tracking.py).
DO $$
DECLARE
    rec RECORD;
    new_container_id BIGINT;
    current_stock NUMERIC(10, 2);
    stock_in_total NUMERIC(10, 2);
    earliest_occurred_at TIMESTAMPTZ;
    initial_units_value NUMERIC(10, 2);
    container_status_value VARCHAR(20);
BEGIN
    FOR rec IN SELECT DISTINCT insulin_id FROM stock_movement LOOP
        SELECT
            COALESCE(SUM(quantity_units), 0),
            COALESCE(SUM(quantity_units) FILTER (WHERE movement_type = 'STOCK_IN'), 0),
            MIN(occurred_at)
        INTO current_stock, stock_in_total, earliest_occurred_at
        FROM stock_movement
        WHERE insulin_id = rec.insulin_id;

        IF stock_in_total > 0 THEN
            initial_units_value := stock_in_total;
        ELSE
            initial_units_value := GREATEST(current_stock, 0);
        END IF;

        IF current_stock <= 0 THEN
            container_status_value := 'EMPTY';
        ELSE
            container_status_value := 'OPEN';
        END IF;

        INSERT INTO insulin_container (insulin_id, initial_units, status, opened_at, created_at)
        VALUES (rec.insulin_id, initial_units_value, container_status_value, earliest_occurred_at, earliest_occurred_at)
        RETURNING id INTO new_container_id;

        UPDATE stock_movement
        SET container_id = new_container_id
        WHERE insulin_id = rec.insulin_id;
    END LOOP;
END $$;

ALTER TABLE stock_movement ALTER COLUMN container_id SET NOT NULL;
