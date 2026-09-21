package com.b4rrhh.rulesystem.agreementprofile.infrastructure.persistence;

import com.b4rrhh.rulesystem.agreementprofile.domain.model.AgreementProfile;
import com.b4rrhh.rulesystem.agreementprofile.domain.port.AgreementProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AgreementProfilePersistenceAdapter implements AgreementProfileRepository {

    private final SpringDataAgreementProfileRepository repository;

    public AgreementProfilePersistenceAdapter(SpringDataAgreementProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AgreementProfile> findByAgreementRuleEntityId(Long agreementRuleEntityId) {
        return repository.findByAgreementRuleEntityId(agreementRuleEntityId)
                .map(this::toDomain);
    }

    @Override
    public AgreementProfile save(Long agreementRuleEntityId, AgreementProfile profile) {
        AgreementProfileEntity entity = repository.findByAgreementRuleEntityId(agreementRuleEntityId)
                .orElse(new AgreementProfileEntity());

        entity.setAgreementRuleEntityId(agreementRuleEntityId);
        entity.setOfficialAgreementNumber(profile.getOfficialAgreementNumber());
        entity.setDisplayName(profile.getDisplayName());
        entity.setShortName(profile.getShortName());
        entity.setAnnualHours(profile.getAnnualHours());
        entity.setExtraPaymentsProrated(profile.isExtraPaymentsProrated());
        entity.setIsActive(profile.isActive());

        AgreementProfileEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private AgreementProfile toDomain(AgreementProfileEntity entity) {
        return new AgreementProfile(
                entity.getOfficialAgreementNumber(),
                entity.getDisplayName(),
                entity.getShortName(),
                entity.getAnnualHours(),
                entity.getExtraPaymentsProrated(),
                entity.getIsActive()
        );
    }
}
