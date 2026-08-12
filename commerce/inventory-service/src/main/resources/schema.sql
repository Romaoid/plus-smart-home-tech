CREATE TABLE IF NOT EXISTS records (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    quantity INT DEFAULT 0 NOT NULL,
    reserved_quantity INT DEFAULT 0 NOT NULL,
    available_quantity INT DEFAULT 0 NOT NULL,
    version BIGINT DEFAULT 1 NOT NULL,
    CONSTRAINT quantity_positive_or_zero CHECK (quantity >= 0),
    CONSTRAINT reserved_quantity_limits CHECK (reserved_quantity <= quantity and reserved_quantity >= 0)
);