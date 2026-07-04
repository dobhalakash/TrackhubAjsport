package com.dreamnest.controller;

import com.dreamnest.dto.request.AddressRequest;
import com.dreamnest.dto.response.AddressResponse;
import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.service.AddressService;
import com.dreamnest.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints for managing the authenticated user's saved addresses.
 */
@RestController
@RequestMapping("/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> getAddresses() {
        return ApiResponse.success(addressService.getAddresses(userId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<AddressResponse> getAddress(@PathVariable Long id) {
        return ApiResponse.success(addressService.getAddress(userId(), id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(@Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.createAddress(userId(), request);
        return new ResponseEntity<>(ApiResponse.success("Address added successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressResponse> updateAddress(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        return ApiResponse.success("Address updated successfully", addressService.updateAddress(userId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAddress(@PathVariable Long id) {
        addressService.deleteAddress(userId(), id);
        return ApiResponse.success("Address deleted successfully", null);
    }

    private Long userId() {
        return SecurityUtil.getCurrentUserId();
    }
}
