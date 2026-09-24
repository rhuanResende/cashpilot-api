package com.desenvolvimento.logica.cashpilot_api.subscription.dto;

public record ProfileRequirement(
        String scope,
        String field,
        String message
) {
}
