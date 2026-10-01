package com.b4rrhh.rulesystem.agreementcategoryprofile.infrastructure.persistence;

import com.b4rrhh.rulesystem.agreementcategoryprofile.domain.model.AgreementCategoryProfile;
import com.b4rrhh.rulesystem.agreementcategoryprofile.domain.model.TipoNomina;
import com.b4rrhh.rulesystem.agreementcategoryprofile.domain.port.AgreementCategoryProfileRepository;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AgreementCategoryProfilePersistenceAdapter implements AgreementCategoryProfileRepository {

    private static final String AGREEMENT_CATEGORY = "AGREEMENT_CATEGORY";

    private final RuleEntityRepository ruleEntityRepository;
    private final SpringDataAgreementCategoryProfileRepository springDataRepository;

    public AgreementCategoryProfilePersistenceAdapter(
            RuleEntityRepository ruleEntityRepository,
            SpringDataAgreementCategoryProfileRepository springDataRepository
    ) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<AgreementCategoryProfile> findByCategoryRuleEntityId(Long categoryRuleEntityId) {
        return springDataRepository.findByAgreementCategoryRuleEntityId(categoryRuleEntityId)
                .map(this::toDomain);
    }

    @Override
    public Optional<String> findGrupoCotizacionCodeByCategoryCode(String ruleSystemCode, String categoryCode) {
        // La categoría la resuelve el puerto, por el nivel de su tipo (backend#157).
        return ruleEntityRepository.findByBusinessKey(ruleSystemCode, AGREEMENT_CATEGORY, categoryCode)
                .map(RuleEntity::getId)
                .flatMap(springDataRepository::findByAgreementCategoryRuleEntityId)
                .map(AgreementCategoryProfileEntity::getGrupoCotizacionCode);
    }

    @Override
    public AgreementCategoryProfile save(Long categoryRuleEntityId, AgreementCategoryProfile profile) {
        AgreementCategoryProfileEntity entity = springDataRepository
                .findByAgreementCategoryRuleEntityId(categoryRuleEntityId)
                .orElseGet(AgreementCategoryProfileEntity::new);

        entity.setAgreementCategoryRuleEntityId(categoryRuleEntityId);
        entity.setGrupoCotizacionCode(profile.getGrupoCotizacionCode());
        entity.setTipoNomina(profile.getTipoNomina().name());

        return toDomain(springDataRepository.save(entity));
    }

    private AgreementCategoryProfile toDomain(AgreementCategoryProfileEntity entity) {
        return new AgreementCategoryProfile(
                entity.getGrupoCotizacionCode(),
                TipoNomina.valueOf(entity.getTipoNomina())
        );
    }
}
