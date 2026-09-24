package com.desenvolvimento.logica.cashpilot_api.subscription.dto;

import java.util.List;

public record SubscriptionReadinessResponse(
        boolean ready,
        List<ProfileRequirement> pendingFields
) {
    public SubscriptionReadinessResponse {
        pendingFields = List.copyOf(pendingFields);
        ready = pendingFields.isEmpty();
    }
}
