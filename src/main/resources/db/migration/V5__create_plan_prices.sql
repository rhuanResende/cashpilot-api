CREATE TABLE plan_prices (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL,

    billing_period VARCHAR(20) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_plan_prices_plan
        FOREIGN KEY (plan_id) REFERENCES plans (id),

    CONSTRAINT ck_plan_prices_billing_period
        CHECK (billing_period IN ('MONTHLY', 'YEARLY')),

    CONSTRAINT ck_plan_prices_amount
        CHECK (amount > 0),

    CONSTRAINT ck_plan_prices_currency
        CHECK (currency = 'BRL')
);

CREATE INDEX idx_plan_prices_plan_id
    ON plan_prices (plan_id);

CREATE UNIQUE INDEX uk_plan_prices_active_period
    ON plan_prices (plan_id, billing_period, currency)
    WHERE active = TRUE;