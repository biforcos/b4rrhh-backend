package com.b4rrhh.rulesystem.agreementcategoryprofile.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataAgreementCategoryProfileRepository
        extends JpaRepository<AgreementCategoryProfileEntity, Long> {

    Optional<AgreementCategoryProfileEntity> findByAgreementCategoryRuleEntityId(Long agreementCategoryRuleEntityId);
}
