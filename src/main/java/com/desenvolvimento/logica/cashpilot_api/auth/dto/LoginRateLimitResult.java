package com.desenvolvimento.logica.cashpilot_api.auth.dto;

import java.time.Duration;

public record LoginRateLimitResult(
        boolean allowed,
        int remaining,
        Duration retryAfter
) {
}
