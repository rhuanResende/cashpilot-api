CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(254) NOT NULL,
    document VARCHAR(11),
    phone VARCHAR(20),

    postal_code VARCHAR(20),
    street VARCHAR(200),
    number VARCHAR(20),
    complement VARCHAR(150),
    neighborhood VARCHAR(100),
    city VARCHAR(100),
    state VARCHAR(2),
    country_code VARCHAR(2),

    password_hash VARCHAR(255) NOT NULL,
    platform_role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_users_name_not_blank
        CHECK (length(trim(name)) > 0),

    CONSTRAINT ck_users_email_not_blank
        CHECK (length(trim(email)) > 0),

    CONSTRAINT ck_users_email_normalized
        CHECK (email = lower(trim(email))),

    CONSTRAINT uk_users_email
        UNIQUE (email),

    CONSTRAINT ck_users_document_format
        CHECK (document ~ '^[0-9]{11}$'),

    CONSTRAINT ck_users_country_code
        CHECK (country_code ~ '^[A-Z]{2}$'),

    CONSTRAINT ck_users_state
        CHECK (state ~ '^[A-Z]{2}$'),

    CONSTRAINT ck_users_platform_role
        CHECK (platform_role IN ('USER', 'MASTER')),

    CONSTRAINT ck_users_status
        CHECK (status IN ('ACTIVE', 'BLOCKED'))
);