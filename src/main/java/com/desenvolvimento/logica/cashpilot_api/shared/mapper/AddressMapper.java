package com.desenvolvimento.logica.cashpilot_api.shared.mapper;

import com.desenvolvimento.logica.cashpilot_api.shared.dto.AddressRequest;
import com.desenvolvimento.logica.cashpilot_api.shared.entity.Address;

public final class AddressMapper {

    private AddressMapper() {
    }

    public static Address toEntity(AddressRequest request) {
        if (request == null) {
            return null;
        }

        return new Address(
                request.postalCode(),
                request.street(),
                request.number(),
                request.complement(),
                request.neighborhood(),
                request.city(),
                request.state(),
                request.countryCode()
        );
    }
}
