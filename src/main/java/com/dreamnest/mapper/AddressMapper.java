package com.dreamnest.mapper;

import com.dreamnest.dto.request.AddressRequest;
import com.dreamnest.dto.response.AddressResponse;
import com.dreamnest.entity.Address;

/**
 * Maps {@link Address} entities to/from DTOs.
 */
public class AddressMapper {

    private AddressMapper() {
    }

    public static AddressResponse toResponse(Address address) {
        if (address == null) {
            return null;
        }
        AddressResponse response = new AddressResponse();
        response.setId(address.getId());
        response.setFullName(address.getFullName());
        response.setPhone(address.getPhone());
        response.setAddressLine1(address.getAddressLine1());
        response.setAddressLine2(address.getAddressLine2());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setPincode(address.getPincode());
        response.setCountry(address.getCountry());
        response.setAddressType(address.getAddressType());
        response.setDefault(address.isDefault());
        return response;
    }

    public static void updateEntity(Address address, AddressRequest request) {
        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        if (request.getCountry() != null) {
            address.setCountry(request.getCountry());
        }
        if (request.getAddressType() != null) {
            address.setAddressType(request.getAddressType());
        }
        if (request.getIsDefault() != null) {
            address.setDefault(request.getIsDefault());
        }
    }
}
