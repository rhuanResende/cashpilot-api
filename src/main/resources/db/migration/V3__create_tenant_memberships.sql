CREATE TABLE tenant_memberships (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tenant_memberships_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),

    CONSTRAINT fk_tenant_memberships_user
        FOREIGN KEY (user_id) REFERENCES users (id),

    CONSTRAINT uk_tenant_memberships_tenant_user
        UNIQUE (tenant_id, user_id),

    CONSTRAINT ck_tenant_memberships_role
        CHECK (role IN ('OWNER', 'ADMIN', 'OPERATOR')),

    CONSTRAINT ck_tenant_memberships_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_tenant_memberships_user_id
    ON tenant_memberships (user_id);

CREATE UNIQUE INDEX uk_tenant_memberships_active_owner
    ON tenant_memberships (tenant_id)
    WHERE role = 'OWNER' AND status = 'ACTIVE';