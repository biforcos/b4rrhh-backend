package com.b4rrhh.employee.contract.infrastructure.persistence;

import com.b4rrhh.employee.contract.application.model.ContractSubtypeCatalogItem;
import com.b4rrhh.employee.contract.application.port.ContractSubtypeCatalogLookupPort;
import com.b4rrhh.employee.shared.infrastructure.persistence.RuleEntityRelationReader;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Los subtipos de un tipo de contrato (backend#115). Cada opción publica su vigencia EFECTIVA, la intersección de contrato,
 * subtipo y la relación: es la única que contesta desde cuándo se puede usar, y la que un cliente
 * no puede ver por ningún otro sitio. Las dos puntas las resuelve el puerto de entidades por el
 * nivel de su tipo (backend#157); ver {@link RuleEntityRelationReader}.
 */
@Component
public class ContractSubtypeCatalogLookupAdapter implements ContractSubtypeCatalogLookupPort {

    private final RuleEntityRelationReader relationReader;

    public ContractSubtypeCatalogLookupAdapter(EntityManager entityManager, RuleEntityRepository ruleEntityRepository) {
        this.relationReader = new RuleEntityRelationReader(
                entityManager,
                ruleEntityRepository,
                "contract_subtype_relation",
                "contract_rule_entity_id", "CONTRACT",
                "subtype_rule_entity_id", "CONTRACT_SUBTYPE"
        );
    }

    @Override
    public List<ContractSubtypeCatalogItem> listActiveSubtypesByContractType(
            String ruleSystemCode,
            String contractTypeCode
    ) {
        return list(ruleSystemCode, contractTypeCode, null);
    }

    @Override
    public List<ContractSubtypeCatalogItem> listActiveSubtypesByContractTypeOnDate(
            String ruleSystemCode,
            String contractTypeCode,
            LocalDate referenceDate
    ) {
        return list(ruleSystemCode, contractTypeCode, referenceDate);
    }

    private List<ContractSubtypeCatalogItem> list(String ruleSystemCode, String contractTypeCode, LocalDate referenceDate) {
        return relationReader.related(ruleSystemCode, contractTypeCode, referenceDate).stream()
                .map(option -> new ContractSubtypeCatalogItem(option.code(), option.name(), option.startDate(), option.endDate()))
                .toList();
    }
}
