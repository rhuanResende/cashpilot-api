CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    email VARCHAR(254) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,

    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_email_verification_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id),

    CONSTRAINT uk_email_verification_tokens_hash
        UNIQUE (token_hash),

    CONSTRAINT ck_email_verification_tokens_hash
        CHECK (token_hash ~ '^[0-9a-f]{64}$'),

    CONSTRAINT ck_email_verification_tokens_email
        CHECK (
            length(trim(email)) > 0
            AND email = lower(trim(email))
        ),

    CONSTRAINT ck_email_verification_tokens_expiration
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_email_verification_tokens_user_id
    ON email_verification_tokens (user_id);

CREATE INDEX idx_email_verification_tokens_expires_at
    ON email_verification_tokens (expires_at);