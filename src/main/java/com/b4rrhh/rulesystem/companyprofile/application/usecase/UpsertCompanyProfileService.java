package com.b4rrhh.rulesystem.companyprofile.application.usecase;

import com.b4rrhh.geo.address.domain.model.PostalAddress;
import com.b4rrhh.geo.address.application.service.PostalAddressValidator;
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
    private final PostalAddressValidator postalAddressValidator;

    public UpsertCompanyProfileService(
            CompanyProfileRepository companyProfileRepository,
            CompanyProfileCompanyResolver companyProfileCompanyResolver,
            CompanyProfileInputNormalizer companyProfileInputNormalizer,
            PostalAddressValidator postalAddressValidator
    ) {
        this.companyProfileRepository = companyProfileRepository;
        this.companyProfileCompanyResolver = companyProfileCompanyResolver;
        this.companyProfileInputNormalizer = companyProfileInputNormalizer;
        this.postalAddressValidator = postalAddressValidator;
    }

    @Override
    @Transactional
    public CompanyProfile upsert(UpsertCompanyProfileCommand command) {
        String ruleSystemCode = companyProfileInputNormalizer.normalizeRequiredRuleSystemCode(command.ruleSystemCode());
        String companyCode = companyProfileInputNormalizer.normalizeRequiredCompanyCode(command.companyCode());

        RuleEntity company = companyProfileCompanyResolver.resolveApplicableToday(ruleSystemCode, companyCode);
        PostalAddress address = command.address() == null
                ? null
                : postalAddressValidator.validate(ruleSystemCode, command.address(), LocalDate.now());

        CompanyProfile requestedProfile = new CompanyProfile(
                command.legalName(),
                command.taxIdentifier(),
                address,
                command.cnaeCode()
        );

        Optional<CompanyProfile> existingProfile = companyProfileRepository.findByCompanyRuleEntityId(company.getId());

        CompanyProfile profileToSave = existingProfile
                .map(existing -> existing.update(
                        requestedProfile.getLegalName(),
                        requestedProfile.getTaxIdentifier(),
                        requestedProfile.getAddress(),
                        requestedProfile.getCnaeCode()
                ))
                .orElse(requestedProfile);

        return companyProfileRepository.save(company.getId(), profileToSave);
    }
}