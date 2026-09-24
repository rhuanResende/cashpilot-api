CREATE TABLE plans (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),

    max_users INTEGER,
    max_products INTEGER,
    trial_duration_days INTEGER,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_plans_code
        UNIQUE (code),

    CONSTRAINT ck_plans_code_format
        CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),

    CONSTRAINT ck_plans_name_not_blank
        CHECK (length(trim(name)) > 0),

    CONSTRAINT ck_plans_max_users
        CHECK (max_users IS NULL OR max_users >= 1),

    CONSTRAINT ck_plans_max_products
        CHECK (max_products IS NULL OR max_products >= 0),

    CONSTRAINT ck_plans_trial_duration
        CHECK (
            trial_duration_days IS NULL
            OR trial_duration_days > 0
        )
);