package com.b4rrhh.rulesystem.companyprofile.infrastructure.web.assembler;

import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import com.b4rrhh.rulesystem.companyprofile.infrastructure.web.dto.CompanyProfileAddressResponse;
import com.b4rrhh.rulesystem.companyprofile.infrastructure.web.dto.CompanyProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class CompanyProfileResponseAssembler {

    public CompanyProfileResponse toResponse(String companyCode, CompanyProfile companyProfile) {
        return new CompanyProfileResponse(
                normalizeRequiredCompanyCode(companyCode),
                companyProfile.getLegalName(),
                companyProfile.getTaxIdentifier(),
                new CompanyProfileAddressResponse(
                        companyProfile.getStreet(),
                        companyProfile.getCity(),
                        companyProfile.getPostalCode(),
                        companyProfile.getRegionCode(),
                        companyProfile.getCountryCode()
                ),
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