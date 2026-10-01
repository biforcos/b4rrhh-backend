package com.b4rrhh.employee.labor_classification.infrastructure.persistence;

import com.b4rrhh.employee.labor_classification.application.model.AgreementCategoryCatalogItem;
import com.b4rrhh.employee.labor_classification.application.port.AgreementCategoryCatalogLookupPort;
import com.b4rrhh.employee.shared.infrastructure.persistence.RuleEntityRelationReader;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Las categorías de un convenio (backend#115). Cada opción publica su vigencia EFECTIVA, la intersección de convenio,
 * categoría y la relación: es la única que contesta desde cuándo se puede usar, y la que un cliente
 * no puede ver por ningún otro sitio. Las dos puntas las resuelve el puerto de entidades por el
 * nivel de su tipo (backend#157); ver {@link RuleEntityRelationReader}.
 */
@Component
public class AgreementCategoryCatalogLookupAdapter implements AgreementCategoryCatalogLookupPort {

    private final RuleEntityRelationReader relationReader;

    public AgreementCategoryCatalogLookupAdapter(EntityManager entityManager, RuleEntityRepository ruleEntityRepository) {
        this.relationReader = new RuleEntityRelationReader(
                entityManager,
                ruleEntityRepository,
                "agreement_category_relation",
                "agreement_rule_entity_id", "AGREEMENT",
                "category_rule_entity_id", "AGREEMENT_CATEGORY"
        );
    }

    @Override
    public List<AgreementCategoryCatalogItem> listActiveCategoriesByAgreement(
            String ruleSystemCode,
            String agreementCode
    ) {
        return list(ruleSystemCode, agreementCode, null);
    }

    @Override
    public List<AgreementCategoryCatalogItem> listActiveCategoriesByAgreementOnDate(
            String ruleSystemCode,
            String agreementCode,
            LocalDate referenceDate
    ) {
        return list(ruleSystemCode, agreementCode, referenceDate);
    }

    private List<AgreementCategoryCatalogItem> list(String ruleSystemCode, String agreementCode, LocalDate referenceDate) {
        return relationReader.related(ruleSystemCode, agreementCode, referenceDate).stream()
                .map(option -> new AgreementCategoryCatalogItem(option.code(), option.name(), option.startDate(), option.endDate()))
                .toList();
    }
}
