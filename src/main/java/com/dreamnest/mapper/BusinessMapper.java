package com.dreamnest.mapper;

import com.dreamnest.dto.response.BusinessProfileResponse;
import com.dreamnest.entity.BusinessProfile;

/**
 * Maps {@link BusinessProfile} entities to response DTOs.
 */
public class BusinessMapper {

    private BusinessMapper() {
    }

    public static BusinessProfileResponse toResponse(BusinessProfile profile) {
        if (profile == null) {
            return null;
        }
        BusinessProfileResponse response = new BusinessProfileResponse();
        response.setId(profile.getId());
        response.setUserId(profile.getUser() != null ? profile.getUser().getId() : null);
        response.setBusinessName(profile.getBusinessName());
        response.setOwnerName(profile.getOwnerName());
        response.setGstNumber(profile.getGstNumber());
        response.setUpiId(profile.getUpiId());
        response.setBankAccountNumber(profile.getBankAccountNumber());
        response.setIfscCode(profile.getIfscCode());
        response.setBankName(profile.getBankName());
        response.setAccountHolderName(profile.getAccountHolderName());
        response.setStatus(profile.getStatus());
        if (profile.getUser() != null) {
            response.setEmail(profile.getUser().getEmail());
            response.setMobile(profile.getUser().getMobileNumber());
        }
        return response;
    }
}
