package com.b4rrhh.rulesystem.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataRuleEntityExtensionRepository
        extends JpaRepository<RuleEntityExtensionEntity, RuleEntityExtensionId> {
}
