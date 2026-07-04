package com.dreamnest.service;

import com.dreamnest.dto.request.AddressRequest;
import com.dreamnest.dto.response.AddressResponse;

import java.util.List;

/**
 * Manages a user's saved addresses.
 */
public interface AddressService {

    List<AddressResponse> getAddresses(Long userId);

    AddressResponse getAddress(Long userId, Long addressId);

    AddressResponse createAddress(Long userId, AddressRequest request);

    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);

    void deleteAddress(Long userId, Long addressId);
}
