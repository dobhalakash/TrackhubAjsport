package com.dreamnest.service.impl;

import com.dreamnest.dto.request.AddressRequest;
import com.dreamnest.dto.response.AddressResponse;
import com.dreamnest.entity.Address;
import com.dreamnest.entity.User;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.exception.UnauthorizedException;
import com.dreamnest.mapper.AddressMapper;
import com.dreamnest.repository.AddressRepository;
import com.dreamnest.repository.UserRepository;
import com.dreamnest.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link AddressService}.
 */
@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressServiceImpl(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(AddressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AddressResponse getAddress(Long userId, Long addressId) {
        Address address = findAddress(userId, addressId);
        return AddressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Address address = new Address();
        address.setUser(user);
        AddressMapper.updateEntity(address, request);

        if (Boolean.TRUE.equals(request.getIsDefault()) || addressRepository.findByUserId(userId).isEmpty()) {
            unsetExistingDefaults(userId);
            address.setDefault(true);
        }

        address = addressRepository.save(address);
        return AddressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = findAddress(userId, addressId);
        AddressMapper.updateEntity(address, request);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetExistingDefaults(userId);
            address.setDefault(true);
        }

        address = addressRepository.save(address);
        return AddressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = findAddress(userId, addressId);
        addressRepository.delete(address);
    }

    private void unsetExistingDefaults(Long userId) {
        List<Address> addresses = addressRepository.findByUserId(userId);
        for (Address a : addresses) {
            if (a.isDefault()) {
                a.setDefault(false);
                addressRepository.save(a);
            }
        }
    }

    private Address findAddress(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
        if (!address.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to access this address");
        }
        return address;
    }
}
