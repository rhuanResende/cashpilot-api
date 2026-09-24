-- Permite garantir que o preço pertence ao plano da assinatura.
ALTER TABLE plan_prices
    ADD CONSTRAINT uk_plan_prices_id_plan
        UNIQUE (id, plan_id);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    plan_price_id UUID,

    status VARCHAR(20) NOT NULL,

    current_period_starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    current_period_ends_at TIMESTAMP WITH TIME ZONE NOT NULL,

    trial_starts_at TIMESTAMP WITH TIME ZONE,
    trial_ends_at TIMESTAMP WITH TIME ZONE,

    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    cancellation_requested_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_subscriptions_tenant
        UNIQUE (tenant_id),

    CONSTRAINT fk_subscriptions_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),

    CONSTRAINT fk_subscriptions_plan
        FOREIGN KEY (plan_id) REFERENCES plans (id),

    CONSTRAINT fk_subscriptions_price_plan
        FOREIGN KEY (plan_price_id, plan_id)
        REFERENCES plan_prices (id, plan_id),

    CONSTRAINT ck_subscriptions_status
        CHECK (
            status IN (
                'TRIALING',
                'ACTIVE',
                'PAST_DUE',
                'EXPIRED',
                'CANCELED'
            )
        ),

    CONSTRAINT ck_subscriptions_current_period
        CHECK (current_period_ends_at > current_period_starts_at),

    CONSTRAINT ck_subscriptions_trial_period
        CHECK (
            (trial_starts_at IS NULL AND trial_ends_at IS NULL)
        OR
            (
                trial_starts_at IS NOT NULL
                AND trial_ends_at IS NOT NULL
                AND trial_ends_at > trial_starts_at
            )
        ),

    CONSTRAINT ck_subscriptions_price_required
        CHECK (
            (status = 'TRIALING' AND plan_price_id IS NULL)
            OR
            (
                status IN ('ACTIVE', 'PAST_DUE')
                AND plan_price_id IS NOT NULL
            )
            OR
            status IN ('EXPIRED', 'CANCELED')
        ),

    CONSTRAINT ck_subscriptions_trialing_dates
        CHECK (
            status <> 'TRIALING'
            OR
            (
                trial_starts_at IS NOT NULL
                AND trial_ends_at IS NOT NULL
                AND current_period_starts_at = trial_starts_at
                AND current_period_ends_at = trial_ends_at
            )
        ),

    CONSTRAINT ck_subscriptions_cancellation
        CHECK (
            (
                cancel_at_period_end = FALSE
                AND cancellation_requested_at IS NULL
            )
            OR
            (
                cancel_at_period_end = TRUE
                AND cancellation_requested_at IS NOT NULL
            )
        )
);

CREATE INDEX idx_subscriptions_plan_id
    ON subscriptions (plan_id);

CREATE INDEX idx_subscriptions_plan_price_id
    ON subscriptions (plan_price_id);