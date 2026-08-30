package com.b4rrhh.rulesystem.company.application.usecase;

import com.b4rrhh.rulesystem.company.domain.exception.CompanyNotApplicableException;
import com.b4rrhh.rulesystem.company.domain.exception.CompanyNotFoundException;
import com.b4rrhh.rulesystem.company.domain.model.Company;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileCatalogValidator;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileCompanyResolver;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileInputNormalizer;
import com.b4rrhh.rulesystem.companyprofile.domain.exception.CompanyProfileCompanyNotApplicableException;
import com.b4rrhh.rulesystem.companyprofile.domain.exception.CompanyProfileCompanyNotFoundException;
import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import com.b4rrhh.rulesystem.companyprofile.domain.port.CompanyProfileRepository;
import com.b4rrhh.rulesystem.domain.exception.RequiredExtensionMissingException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class UpdateCompanyService implements UpdateCompanyUseCase {

    private final CompanyProfileCompanyResolver companyResolver;
    private final CompanyProfileInputNormalizer inputNormalizer;
    private final CompanyProfileCatalogValidator catalogValidator;
    private final RuleEntityRepository ruleEntityRepository;
    private final CompanyProfileRepository companyProfileRepository;

    public UpdateCompanyService(
            CompanyProfileCompanyResolver companyResolver,
            CompanyProfileInputNormalizer inputNormalizer,
            CompanyProfileCatalogValidator catalogValidator,
            RuleEntityRepository ruleEntityRepository,
            CompanyProfileRepository companyProfileRepository
    ) {
        this.companyResolver = companyResolver;
        this.inputNormalizer = inputNormalizer;
        this.catalogValidator = catalogValidator;
        this.ruleEntityRepository = ruleEntityRepository;
        this.companyProfileRepository = companyProfileRepository;
    }

    @Override
    @Transactional
    public Company update(UpdateCompanyCommand command) {
        String ruleSystemCode = inputNormalizer.normalizeRequiredRuleSystemCode(command.ruleSystemCode());
        String companyCode = inputNormalizer.normalizeRequiredCompanyCode(command.companyCode());
        String countryCode = inputNormalizer.normalizeOptionalCountryCode(command.countryCode());

        RuleEntity companyEntity = resolveApplicableCompany(ruleSystemCode, companyCode);
        catalogValidator.validateCountryCode(ruleSystemCode, countryCode, LocalDate.now());

        String name = normalizeRequiredText("name", command.name(), 100);
        String description = normalizeOptionalText("description", command.description(), 500);

        companyEntity.correct(name, description, companyEntity.getEndDate());
        RuleEntity savedCompanyEntity = ruleEntityRepository.save(companyEntity);

        CompanyProfile existingProfile = companyProfileRepository
                .findByCompanyRuleEntityId(savedCompanyEntity.getId())
                .orElseThrow(() -> new RequiredExtensionMissingException(savedCompanyEntity.getId(), "rulesystem.company_profile"));

        CompanyProfile updatedProfile = existingProfile.update(
                command.legalName(),
                command.taxIdentifier(),
                command.street(),
                command.city(),
                command.postalCode(),
                command.regionCode(),
                countryCode,
                null
        );

        CompanyProfile savedProfile = companyProfileRepository.save(savedCompanyEntity.getId(), updatedProfile);
        return toCompany(savedCompanyEntity, savedProfile);
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

    private String normalizeRequiredText(String fieldName, String value, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " exceeds max length " + maxLength);
        }

        return normalized;
    }

    private String normalizeOptionalText(String fieldName, String value, int maxLength) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " exceeds max length " + maxLength);
        }

        return normalized;
    }
}
