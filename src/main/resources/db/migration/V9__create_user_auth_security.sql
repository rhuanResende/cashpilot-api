CREATE TABLE user_auth_security (
    user_id UUID PRIMARY KEY,

    password_failed_attempts INTEGER NOT NULL DEFAULT 0,
    password_failure_window_started_at TIMESTAMP WITH TIME ZONE,
    password_locked_until TIMESTAMP WITH TIME ZONE,
    last_password_failure_at TIMESTAMP WITH TIME ZONE,

    mfa_failed_attempts INTEGER NOT NULL DEFAULT 0,
    mfa_failure_window_started_at TIMESTAMP WITH TIME ZONE,
    mfa_locked_until TIMESTAMP WITH TIME ZONE,
    last_mfa_failure_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_auth_security_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT ck_user_auth_security_password_attempts
        CHECK (password_failed_attempts >= 0),

    CONSTRAINT ck_user_auth_security_mfa_attempts
        CHECK (mfa_failed_attempts >= 0),

    CONSTRAINT ck_user_auth_security_password_window
        CHECK (
            (
                password_failed_attempts = 0
                    AND password_failure_window_started_at IS NULL
                )
                OR
            (
                password_failed_attempts > 0
                    AND password_failure_window_started_at IS NOT NULL
                )
            ),

    CONSTRAINT ck_user_auth_security_mfa_window
        CHECK (
            (
                mfa_failed_attempts = 0
                    AND mfa_failure_window_started_at IS NULL
                )
                OR
            (
                mfa_failed_attempts > 0
                    AND mfa_failure_window_started_at IS NOT NULL
                )
            )
);

INSERT INTO user_auth_security (user_id)
SELECT id
FROM users;

ALTER TABLE users
    ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE;