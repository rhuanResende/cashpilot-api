package com.desenvolvimento.logica.cashpilot_api.shared.mapper;

import com.desenvolvimento.logica.cashpilot_api.shared.dto.AddressRequest;
import com.desenvolvimento.logica.cashpilot_api.shared.model.Address;

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

    public static AddressRequest toRequest(Address address) {
        if (address == null) {
            return null;
        }

        return new AddressRequest(
                address.getPostalCode(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getNeighborhood(),
                address.getCity(),
                address.getState(),
                address.getCountryCode()
        );
    }
}
