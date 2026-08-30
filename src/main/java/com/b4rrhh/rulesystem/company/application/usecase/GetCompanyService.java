package com.b4rrhh.rulesystem.company.application.usecase;

import com.b4rrhh.rulesystem.company.domain.exception.CompanyNotApplicableException;
import com.b4rrhh.rulesystem.company.domain.exception.CompanyNotFoundException;
import com.b4rrhh.rulesystem.company.domain.model.Company;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileCompanyResolver;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileInputNormalizer;
import com.b4rrhh.rulesystem.companyprofile.domain.exception.CompanyProfileCompanyNotApplicableException;
import com.b4rrhh.rulesystem.companyprofile.domain.exception.CompanyProfileCompanyNotFoundException;
import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import com.b4rrhh.rulesystem.companyprofile.domain.port.CompanyProfileRepository;
import com.b4rrhh.rulesystem.domain.exception.RequiredExtensionMissingException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCompanyService implements GetCompanyUseCase {

    private final CompanyProfileCompanyResolver companyResolver;
    private final CompanyProfileInputNormalizer inputNormalizer;
    private final CompanyProfileRepository companyProfileRepository;

    public GetCompanyService(
            CompanyProfileCompanyResolver companyResolver,
            CompanyProfileInputNormalizer inputNormalizer,
            CompanyProfileRepository companyProfileRepository
    ) {
        this.companyResolver = companyResolver;
        this.inputNormalizer = inputNormalizer;
        this.companyProfileRepository = companyProfileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Company get(GetCompanyQuery query) {
        String ruleSystemCode = inputNormalizer.normalizeRequiredRuleSystemCode(query.ruleSystemCode());
        String companyCode = inputNormalizer.normalizeRequiredCompanyCode(query.companyCode());

        RuleEntity companyEntity = resolveApplicableCompany(ruleSystemCode, companyCode);

        CompanyProfile companyProfile = companyProfileRepository
            .findByCompanyRuleEntityId(companyEntity.getId())
            .orElseThrow(() -> new RequiredExtensionMissingException(companyEntity.getId(), "rulesystem.company_profile"));

        return toCompany(companyEntity, companyProfile);
    }

    private RuleEntity resolveApplicableCompany(String ruleSystemCode, String companyCode) {
        try {
            return companyResolver.resolveApplicableToday(ruleSystemCode, companyCode);
        } catch (CompanyProfileCompanyNotFoundException ex) {
            throw new CompanyNotFoundException(ruleSystemCode, companyCode);
        } catch (CompanyProfileCompanyNotApplicableException ex) {
            throw new CompanyNotApplicableException(ruleSystemCode, companyCode, java.time.LocalDate.now());
        }
    }

    private Company toCompany(RuleEntity companyEntity, CompanyProfile companyProfile) {
        return new Company(
                companyEntity.getRuleSystemCode(),
                companyEntity.getCode(),
                companyEntity.getName(),
                companyEntity.getDescription(),
                companyEntity.getStartDate(),
                companyEntity.getEndDate(),
                companyEntity.isActive(),
                companyProfile.getLegalName(),
                companyProfile.getTaxIdentifier(),
                companyProfile.getStreet(),
                companyProfile.getCity(),
                companyProfile.getPostalCode(),
                companyProfile.getRegionCode(),
                companyProfile.getCountryCode()
        );
    }

}
