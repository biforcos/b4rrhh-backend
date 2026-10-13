package com.b4rrhh.rulesystem.companyprofile.infrastructure.web.assembler;

import com.b4rrhh.geo.address.infrastructure.web.dto.PostalAddressResponse;
import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import com.b4rrhh.rulesystem.companyprofile.infrastructure.web.dto.CompanyProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class CompanyProfileResponseAssembler {

    public CompanyProfileResponse toResponse(String companyCode, CompanyProfile companyProfile) {
        return new CompanyProfileResponse(
                normalizeRequiredCompanyCode(companyCode),
                companyProfile.getLegalName(),
                companyProfile.getTaxIdentifier(),
                PostalAddressResponse.ofOrEmpty(companyProfile.getAddress()),
                companyProfile.getCnaeCode()
        );
    }

    private String normalizeRequiredCompanyCode(String companyCode) {
        if (companyCode == null || companyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("companyCode is required");
        }

        return companyCode.trim().toUpperCase();
    }
}