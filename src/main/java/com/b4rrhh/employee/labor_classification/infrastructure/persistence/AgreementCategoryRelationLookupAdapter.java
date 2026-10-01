package com.b4rrhh.employee.labor_classification.infrastructure.persistence;

import com.b4rrhh.employee.labor_classification.application.port.AgreementCategoryRelationLookupPort;
import com.b4rrhh.employee.shared.infrastructure.persistence.RuleEntityRelationReader;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Si la categoría pertenece al convenio. Las dos puntas las resuelve el puerto de entidades por el nivel de su tipo
 * (backend#157); ver {@link RuleEntityRelationReader}.
 */
@Component
public class AgreementCategoryRelationLookupAdapter implements AgreementCategoryRelationLookupPort {

    private final RuleEntityRelationReader relationReader;

    public AgreementCategoryRelationLookupAdapter(EntityManager entityManager, RuleEntityRepository ruleEntityRepository) {
        this.relationReader = new RuleEntityRelationReader(
                entityManager,
                ruleEntityRepository,
                "agreement_category_relation",
                "agreement_rule_entity_id", "AGREEMENT",
                "category_rule_entity_id", "AGREEMENT_CATEGORY"
        );
    }

    @Override
    public boolean existsActiveRelation(
            String ruleSystemCode,
            String agreementCode,
            String agreementCategoryCode,
            LocalDate referenceDate
    ) {
        return relationReader.related(ruleSystemCode, agreementCode, referenceDate).stream()
                .anyMatch(option -> option.code().equals(agreementCategoryCode));
    }
}
