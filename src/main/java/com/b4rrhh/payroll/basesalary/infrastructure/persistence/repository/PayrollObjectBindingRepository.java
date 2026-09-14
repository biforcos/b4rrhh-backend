package com.b4rrhh.payroll.basesalary.infrastructure.persistence.repository;

import com.b4rrhh.payroll.basesalary.infrastructure.persistence.entity.PayrollObjectBindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for PayrollObjectBindingEntity.
 * Persistence port.
 */
public interface PayrollObjectBindingRepository extends JpaRepository<PayrollObjectBindingEntity, Long> {

    /**
     * Find binding by rule system, owner, and binding role.
     */
        Optional<PayrollObjectBindingEntity> findByRuleSystemCodeAndOwnerTypeCodeAndOwnerCodeAndBindingRoleCodeAndBoundObjectTypeCodeAndActiveTrue(
            String ruleSystemCode,
            String ownerTypeCode,
            String ownerCode,
            String bindingRoleCode,
            String boundObjectTypeCode
    );

    /**
     * Todas las vinculaciones de un sistema de reglas que apuntan a un tipo de
     * objeto, activas e inactivas. Las inactivas tambien: una vinculacion
     * apagada es la explicacion de una tabla que dejo de leerse (backend#95).
     */
    List<PayrollObjectBindingEntity> findByRuleSystemCodeAndBoundObjectTypeCode(
            String ruleSystemCode,
            String boundObjectTypeCode
    );
}
