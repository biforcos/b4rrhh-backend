package com.b4rrhh.rulesystem.companyprofile.infrastructure.persistence;

import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import com.b4rrhh.rulesystem.companyprofile.domain.port.CompanyProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CompanyProfilePersistenceAdapter implements CompanyProfileRepository {

    private final SpringDataCompanyProfileRepository springDataCompanyProfileRepository;

    public CompanyProfilePersistenceAdapter(SpringDataCompanyProfileRepository springDataCompanyProfileRepository) {
        this.springDataCompanyProfileRepository = springDataCompanyProfileRepository;
    }

    @Override
    public Optional<CompanyProfile> findByCompanyRuleEntityId(Long companyRuleEntityId) {
        return springDataCompanyProfileRepository.findByCompanyRuleEntityId(companyRuleEntityId)
                .map(this::toDomain);
    }

    @Override
    public CompanyProfile save(Long companyRuleEntityId, CompanyProfile companyProfile) {
        CompanyProfileEntity entity = springDataCompanyProfileRepository.findByCompanyRuleEntityId(companyRuleEntityId)
                .orElseGet(CompanyProfileEntity::new);

        entity.setCompanyRuleEntityId(companyRuleEntityId);
        entity.setLegalName(companyProfile.getLegalName());
        entity.setTaxIdentifier(companyProfile.getTaxIdentifier());
        entity.setStreet(companyProfile.getStreet());
        entity.setCity(companyProfile.getCity());
        entity.setPostalCode(companyProfile.getPostalCode());
        entity.setRegionCode(companyProfile.getRegionCode());
        entity.setCountryCode(companyProfile.getCountryCode());
        entity.setCnaeCode(companyProfile.getCnaeCode());
        entity.setCnaeClassification(companyProfile.getCnaeClassification());

        CompanyProfileEntity saved = springDataCompanyProfileRepository.save(entity);
        return toDomain(saved);
    }

    private CompanyProfile toDomain(CompanyProfileEntity entity) {
        return new CompanyProfile(
                entity.getLegalName(),
                entity.getTaxIdentifier(),
                entity.getStreet(),
                entity.getCity(),
                entity.getPostalCode(),
                entity.getRegionCode(),
                entity.getCountryCode(),
                entity.getCnaeCode()
        );
    }
}