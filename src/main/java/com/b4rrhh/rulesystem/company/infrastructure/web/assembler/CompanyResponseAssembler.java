package com.b4rrhh.rulesystem.company.infrastructure.web.assembler;

import com.b4rrhh.geo.address.infrastructure.web.dto.PostalAddressResponse;
import com.b4rrhh.rulesystem.company.domain.model.Company;
import com.b4rrhh.rulesystem.company.infrastructure.web.dto.CompanyListItemResponse;
import com.b4rrhh.rulesystem.company.infrastructure.web.dto.CompanyResponse;
import org.springframework.stereotype.Component;

@Component
public class CompanyResponseAssembler {

    public CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.ruleSystemCode(),
                company.companyCode(),
                company.name(),
                company.description(),
                company.startDate(),
                company.endDate(),
                company.active(),
                company.legalName(),
                company.taxIdentifier(),
                PostalAddressResponse.ofOrEmpty(company.address()),
                company.cnaeCode()
        );
    }

    public CompanyListItemResponse toListItemResponse(Company company) {
        return new CompanyListItemResponse(
                company.ruleSystemCode(),
                company.companyCode(),
                company.name(),
                company.legalName(),
                company.taxIdentifier(),
                company.address() == null ? null : company.address().countryCode(),
                company.active(),
                company.startDate(),
                company.endDate()
        );
    }
}
