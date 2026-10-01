package com.b4rrhh.employee.contract.infrastructure.persistence;

import com.b4rrhh.employee.contract.application.port.ContractSubtypeRelationLookupPort;
import com.b4rrhh.employee.shared.infrastructure.persistence.RuleEntityRelationReader;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Si el subtipo pertenece al tipo de contrato. Las dos puntas las resuelve el puerto de entidades por el nivel de su tipo
 * (backend#157); ver {@link RuleEntityRelationReader}.
 */
@Component
public class ContractSubtypeRelationLookupAdapter implements ContractSubtypeRelationLookupPort {

    private final RuleEntityRelationReader relationReader;

    public ContractSubtypeRelationLookupAdapter(EntityManager entityManager, RuleEntityRepository ruleEntityRepository) {
        this.relationReader = new RuleEntityRelationReader(
                entityManager,
                ruleEntityRepository,
                "contract_subtype_relation",
                "contract_rule_entity_id", "CONTRACT",
                "subtype_rule_entity_id", "CONTRACT_SUBTYPE"
        );
    }

    @Override
    public boolean existsActiveRelation(
            String ruleSystemCode,
            String contractCode,
            String contractSubtypeCode,
            LocalDate referenceDate
    ) {
        return relationReader.related(ruleSystemCode, contractCode, referenceDate).stream()
                .anyMatch(option -> option.code().equals(contractSubtypeCode));
    }
}
