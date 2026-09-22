package com.b4rrhh.rulesystem.company.application.usecase;

import com.b4rrhh.rulesystem.company.domain.exception.CompanyAlreadyExistsException;
import com.b4rrhh.rulesystem.company.domain.model.Company;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileCatalogValidator;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileInputNormalizer;
import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import com.b4rrhh.rulesystem.companyprofile.domain.port.CompanyProfileRepository;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityTypeRepository;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class CreateCompanyService implements CreateCompanyUseCase {

    private final RuleEntityRepository ruleEntityRepository;
    private final RuleSystemRepository ruleSystemRepository;
    private final RuleEntityTypeRepository ruleEntityTypeRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final CompanyProfileInputNormalizer inputNormalizer;
    private final CompanyProfileCatalogValidator catalogValidator;

    public CreateCompanyService(
            RuleEntityRepository ruleEntityRepository,
            RuleSystemRepository ruleSystemRepository,
            RuleEntityTypeRepository ruleEntityTypeRepository,
            CompanyProfileRepository companyProfileRepository,
            CompanyProfileInputNormalizer inputNormalizer,
            CompanyProfileCatalogValidator catalogValidator
    ) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.ruleSystemRepository = ruleSystemRepository;
        this.ruleEntityTypeRepository = ruleEntityTypeRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.inputNormalizer = inputNormalizer;
        this.catalogValidator = catalogValidator;
    }

    @Override
    @Transactional
    public Company create(CreateCompanyCommand command) {
        String ruleSystemCode = inputNormalizer.normalizeRequiredRuleSystemCode(command.ruleSystemCode());
        String companyCode = inputNormalizer.normalizeRequiredCompanyCode(command.companyCode());
        String countryCode = inputNormalizer.normalizeOptionalCountryCode(command.countryCode());

        LocalDate startDate = requireStartDate(command.startDate());
        String name = normalizeRequiredText("name", command.name(), 100);
        String description = normalizeOptionalText("description", command.description(), 500);

        ruleSystemRepository.findByCode(ruleSystemCode).orElseThrow(
                () -> new IllegalArgumentException("Rule system not found with code: " + ruleSystemCode)
        );

        ruleEntityTypeRepository.findByCode(CompanyRuleEntityTypeCodes.COMPANY).orElseThrow(
                () -> new IllegalArgumentException("Rule entity type not found with code: " + CompanyRuleEntityTypeCodes.COMPANY)
        );

        ruleEntityRepository.findByBusinessKey(ruleSystemCode, CompanyRuleEntityTypeCodes.COMPANY, companyCode)
                .ifPresent(existing -> {
                    throw new CompanyAlreadyExistsException(ruleSystemCode, companyCode);
                });

        catalogValidator.validateCountryCode(ruleSystemCode, countryCode, startDate);

        RuleEntity companyEntity = new RuleEntity(
                null,
                ruleSystemCode,
                CompanyRuleEntityTypeCodes.COMPANY,
                companyCode,
                name,
                description,
                true,
                startDate,
                null,
                null,
                null
        );

        RuleEntity savedCompanyEntity = ruleEntityRepository.save(companyEntity);

        CompanyProfile companyProfile = new CompanyProfile(
                command.legalName(),
                command.taxIdentifier(),
                command.street(),
                command.city(),
                command.postalCode(),
                command.regionCode(),
                countryCode,
                command.cnaeCode()
        );

        Company savedProfile = toCompany(savedCompanyEntity, companyProfileRepository.save(savedCompanyEntity.getId(), companyProfile));
        return savedProfile;
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
                companyProfile.getCountryCode(),
                companyProfile.getCnaeCode()
        );
    }

    private LocalDate requireStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }

        return startDate;
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
