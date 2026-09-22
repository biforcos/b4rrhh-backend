package com.b4rrhh.rulesystem.companyprofile.application.usecase;

import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileCatalogValidator;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileCompanyResolver;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileInputNormalizer;
import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import com.b4rrhh.rulesystem.companyprofile.domain.port.CompanyProfileRepository;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class UpsertCompanyProfileService implements UpsertCompanyProfileUseCase {

    private final CompanyProfileRepository companyProfileRepository;
    private final CompanyProfileCompanyResolver companyProfileCompanyResolver;
    private final CompanyProfileInputNormalizer companyProfileInputNormalizer;
    private final CompanyProfileCatalogValidator companyProfileCatalogValidator;

    public UpsertCompanyProfileService(
            CompanyProfileRepository companyProfileRepository,
            CompanyProfileCompanyResolver companyProfileCompanyResolver,
            CompanyProfileInputNormalizer companyProfileInputNormalizer,
            CompanyProfileCatalogValidator companyProfileCatalogValidator
    ) {
        this.companyProfileRepository = companyProfileRepository;
        this.companyProfileCompanyResolver = companyProfileCompanyResolver;
        this.companyProfileInputNormalizer = companyProfileInputNormalizer;
        this.companyProfileCatalogValidator = companyProfileCatalogValidator;
    }

    @Override
    @Transactional
    public CompanyProfile upsert(UpsertCompanyProfileCommand command) {
        String ruleSystemCode = companyProfileInputNormalizer.normalizeRequiredRuleSystemCode(command.ruleSystemCode());
        String companyCode = companyProfileInputNormalizer.normalizeRequiredCompanyCode(command.companyCode());
        String countryCode = companyProfileInputNormalizer.normalizeOptionalCountryCode(command.countryCode());

        RuleEntity company = companyProfileCompanyResolver.resolveApplicableToday(ruleSystemCode, companyCode);
        companyProfileCatalogValidator.validateCountryCode(ruleSystemCode, countryCode, LocalDate.now());

        CompanyProfile requestedProfile = new CompanyProfile(
                command.legalName(),
                command.taxIdentifier(),
                command.street(),
                command.city(),
                command.postalCode(),
                command.regionCode(),
                countryCode,
                command.cnaeCode()
        );

        Optional<CompanyProfile> existingProfile = companyProfileRepository.findByCompanyRuleEntityId(company.getId());

        CompanyProfile profileToSave = existingProfile
                .map(existing -> existing.update(
                        requestedProfile.getLegalName(),
                        requestedProfile.getTaxIdentifier(),
                        requestedProfile.getStreet(),
                        requestedProfile.getCity(),
                        requestedProfile.getPostalCode(),
                        requestedProfile.getRegionCode(),
                        requestedProfile.getCountryCode(),
                        requestedProfile.getCnaeCode()
                ))
                .orElse(requestedProfile);

        return companyProfileRepository.save(company.getId(), profileToSave);
    }
}