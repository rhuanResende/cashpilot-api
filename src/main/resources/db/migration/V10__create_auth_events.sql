CREATE TABLE auth_events (
     id UUID PRIMARY KEY,

     occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
         DEFAULT CURRENT_TIMESTAMP,

     event_type VARCHAR(64) NOT NULL,
     outcome VARCHAR(20) NOT NULL,
     reason_code VARCHAR(64),

     user_id UUID,
     actor_user_id UUID,
     tenant_id UUID,

     source_ip VARCHAR(45),
     user_agent VARCHAR(512),
     request_id UUID NOT NULL,

     CONSTRAINT ck_auth_events_event_type
         CHECK (event_type ~ '^[A-Z][A-Z0-9_]{0,63}$'),

    CONSTRAINT ck_auth_events_outcome
        CHECK (outcome IN ('SUCCESS', 'FAILURE', 'DENIED')),

    CONSTRAINT ck_auth_events_reason_code
        CHECK (
            reason_code IS NULL
            OR reason_code ~ '^[A-Z][A-Z0-9_]{0,63}$'
        ),

    CONSTRAINT ck_auth_events_source_ip_not_blank
        CHECK (
            source_ip IS NULL
            OR length(trim(source_ip)) > 0
        )
);

CREATE INDEX idx_auth_events_user_occurred_at
    ON auth_events (user_id, occurred_at DESC);

CREATE INDEX idx_auth_events_actor_occurred_at
    ON auth_events (actor_user_id, occurred_at DESC);

CREATE INDEX idx_auth_events_tenant_occurred_at
    ON auth_events (tenant_id, occurred_at DESC);

CREATE INDEX idx_auth_events_request_id
    ON auth_events (request_id);

CREATE INDEX idx_auth_events_occurred_at
    ON auth_events (occurred_at);