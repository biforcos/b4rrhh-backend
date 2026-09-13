package com.b4rrhh.rulesystem.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataRuleEntityExtensionRepository
        extends JpaRepository<RuleEntityExtensionEntity, RuleEntityExtensionId> {

    List<RuleEntityExtensionEntity> findByRuleEntityTypeCodeAndRequiredIsTrueOrderByExtensionCode(
            String ruleEntityTypeCode);
}
