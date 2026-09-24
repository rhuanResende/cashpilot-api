CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    legal_name VARCHAR(200),

    document_type VARCHAR(10),
    document VARCHAR(14),

    contact_email VARCHAR(254),
    phone VARCHAR(20),

    postal_code VARCHAR(20),
    street VARCHAR(200),
    number VARCHAR(20),
    complement VARCHAR(150),
    neighborhood VARCHAR(100),
    city VARCHAR(100),
    state VARCHAR(2),
    country_code VARCHAR(2),

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_tenants_name_not_blank
        CHECK (length(trim(name)) > 0),

    CONSTRAINT ck_tenants_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE')),

    CONSTRAINT ck_tenants_document_type
        CHECK (document_type IN ('CPF', 'CNPJ')),

    CONSTRAINT ck_tenants_document_pair
        CHECK (
            (document_type IS NULL AND document IS NULL)
            OR
            (document_type IS NOT NULL AND document IS NOT NULL)
        ),

    CONSTRAINT ck_tenants_document_format
        CHECK (
            (document_type = 'CPF' AND document ~ '^[0-9]{11}$')
            OR
            (document_type = 'CNPJ' AND document ~ '^[A-Z0-9]{12}[0-9]{2}$')
        ),

    CONSTRAINT ck_tenants_country_code
        CHECK (country_code ~ '^[A-Z]{2}$'),

    CONSTRAINT ck_tenants_state
        CHECK (state ~ '^[A-Z]{2}$')
);