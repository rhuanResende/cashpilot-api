CREATE TABLE tenant_trials (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tenant_trials_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),

    CONSTRAINT uk_tenant_trials_tenant
        UNIQUE (tenant_id),

    CONSTRAINT ck_tenant_trials_period
        CHECK (ends_at > starts_at)
);