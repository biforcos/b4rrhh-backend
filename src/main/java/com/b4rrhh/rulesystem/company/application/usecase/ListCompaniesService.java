package com.b4rrhh.rulesystem.company.application.usecase;

import com.b4rrhh.rulesystem.company.domain.model.Company;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileInputNormalizer;
import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import com.b4rrhh.rulesystem.companyprofile.domain.port.CompanyProfileRepository;
import com.b4rrhh.rulesystem.domain.exception.RequiredExtensionMissingException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ListCompaniesService implements ListCompaniesUseCase {

    private final RuleEntityRepository ruleEntityRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final CompanyProfileInputNormalizer inputNormalizer;

    public ListCompaniesService(
            RuleEntityRepository ruleEntityRepository,
            CompanyProfileRepository companyProfileRepository,
            CompanyProfileInputNormalizer inputNormalizer
    ) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.inputNormalizer = inputNormalizer;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> list(ListCompaniesQuery query) {
        String normalizedRuleSystemCode = normalizeOptionalRuleSystemCode(query.ruleSystemCode());

        return ruleEntityRepository
                .findByFilters(normalizedRuleSystemCode, CompanyRuleEntityTypeCodes.COMPANY, null, true, LocalDate.now())
                .stream()
                .map(this::toCompany)
                .toList();
    }

    private String normalizeOptionalRuleSystemCode(String ruleSystemCode) {
        if (ruleSystemCode == null || ruleSystemCode.trim().isEmpty()) {
            return null;
        }

        return inputNormalizer.normalizeRequiredRuleSystemCode(ruleSystemCode);
    }

    private Company toCompany(RuleEntity companyEntity) {
        Optional<CompanyProfile> profile = companyProfileRepository.findByCompanyRuleEntityId(companyEntity.getId());

        String legalName = profile.map(CompanyProfile::getLegalName)
                .orElseThrow(() -> new RequiredExtensionMissingException(companyEntity.getId(), "rulesystem.company_profile"));
        String taxIdentifier = profile.map(CompanyProfile::getTaxIdentifier).orElse(null);
        String street = profile.map(CompanyProfile::getStreet).orElse(null);
        String city = profile.map(CompanyProfile::getCity).orElse(null);
        String postalCode = profile.map(CompanyProfile::getPostalCode).orElse(null);
        String regionCode = profile.map(CompanyProfile::getRegionCode).orElse(null);
        String countryCode = profile.map(CompanyProfile::getCountryCode).orElse(null);

        return new Company(
                companyEntity.getRuleSystemCode(),
                companyEntity.getCode(),
                companyEntity.getName(),
                companyEntity.getDescription(),
                companyEntity.getStartDate(),
                companyEntity.getEndDate(),
                companyEntity.isActive(),
                legalName,
                taxIdentifier,
                street,
                city,
                postalCode,
                regionCode,
                countryCode
        );
    }
}
