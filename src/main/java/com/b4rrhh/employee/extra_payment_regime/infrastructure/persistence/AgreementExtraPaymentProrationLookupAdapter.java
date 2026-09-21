package com.b4rrhh.employee.extra_payment_regime.infrastructure.persistence;

import com.b4rrhh.employee.extra_payment_regime.application.port.AgreementExtraPaymentProrationLookupPort;
import com.b4rrhh.rulesystem.agreementprofile.application.port.AgreementCatalogLookupPort;
import com.b4rrhh.rulesystem.agreementprofile.infrastructure.persistence.SpringDataAgreementProfileRepository;
import org.springframework.stereotype.Component;

/**
 * Lee el testigo del convenio del perfil de convenio, por las mismas dos consultas que la jornada
 * usa para sus horas anuales: la clave de negocio del convenio resuelve el rule entity, y el
 * perfil cuelga de el.
 */
@Component
public class AgreementExtraPaymentProrationLookupAdapter implements AgreementExtraPaymentProrationLookupPort {

    private final AgreementCatalogLookupPort agreementCatalogLookupPort;
    private final SpringDataAgreementProfileRepository agreementProfileRepository;

    public AgreementExtraPaymentProrationLookupAdapter(
            AgreementCatalogLookupPort agreementCatalogLookupPort,
            SpringDataAgreementProfileRepository agreementProfileRepository
    ) {
        this.agreementCatalogLookupPort = agreementCatalogLookupPort;
        this.agreementProfileRepository = agreementProfileRepository;
    }

    @Override
    public boolean resolveProratedByDefault(String ruleSystemCode, String agreementCode) {
        Long ruleEntityId = agreementCatalogLookupPort
                .findAgreementRuleEntityId(ruleSystemCode, agreementCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Agreement not found: " + ruleSystemCode + "/" + agreementCode
                ));

        return agreementProfileRepository
                .findByAgreementRuleEntityId(ruleEntityId)
                .map(entity -> Boolean.TRUE.equals(entity.getExtraPaymentsProrated()))
                .orElseThrow(() -> new IllegalStateException(
                        "Agreement profile not found for " + ruleSystemCode + "/" + agreementCode
                                + ". The extra payment proration flag must be configured in agreement_profile."
                ));
    }
}
