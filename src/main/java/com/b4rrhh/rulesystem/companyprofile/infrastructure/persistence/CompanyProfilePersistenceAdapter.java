package com.b4rrhh.rulesystem.companyprofile.infrastructure.persistence;

import com.b4rrhh.geo.address.infrastructure.persistence.PostalAddressColumns;
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
        entity.setAddress(PostalAddressColumns.of(companyProfile.getAddress()));
        entity.setCnaeCode(companyProfile.getCnaeCode());
        entity.setCnaeClassification(companyProfile.getCnaeClassification());

        CompanyProfileEntity saved = springDataCompanyProfileRepository.save(entity);
        // La dirección tal y como se escribió: el nombre del municipio es una fórmula que Hibernate
        // no lee hasta volver a cargar la fila.
        return new CompanyProfile(saved.getLegalName(), saved.getTaxIdentifier(), companyProfile.getAddress(), saved.getCnaeCode());
    }

    private CompanyProfile toDomain(CompanyProfileEntity entity) {
        return new CompanyProfile(
                entity.getLegalName(),
                entity.getTaxIdentifier(),
                entity.getAddress() == null ? null : entity.getAddress().toDomainOrNull(),
                entity.getCnaeCode()
        );
    }
}