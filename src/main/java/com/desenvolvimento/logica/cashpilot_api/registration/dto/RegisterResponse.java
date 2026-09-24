package com.desenvolvimento.logica.cashpilot_api.registration.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        UUID tenantId
) {
}
