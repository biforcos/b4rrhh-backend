package com.b4rrhh.employee.presence.infrastructure.web.assembler;

import com.b4rrhh.employee.presence.application.usecase.PresenceRuleEntityTypeCodes;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.presence.infrastructure.web.dto.PresenceResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convierte los códigos de la presencia en literales, en el idioma de la respuesta. La
 * conversión ocurre aquí, en la capa web, y por eso el idioma llega al resolutor sin que
 * ningún caso de uso cambie de firma (ADR-052 §4).
 */
@Component
public class PresenceResponseAssembler {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;

    public PresenceResponseAssembler(RuleEntityLabelResolver ruleEntityLabelResolver) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
    }

    public PresenceResponse toResponse(String ruleSystemCode, Presence presence, ResponseLanguage language) {
        String companyName = label(ruleSystemCode, PresenceRuleEntityTypeCodes.COMPANY,
                presence.getCompanyCode(), language);
        String entryReasonName = label(ruleSystemCode, PresenceRuleEntityTypeCodes.EMPLOYEE_PRESENCE_ENTRY_REASON,
                presence.getEntryReasonCode(), language);
        String exitReasonName = label(ruleSystemCode, PresenceRuleEntityTypeCodes.EMPLOYEE_PRESENCE_EXIT_REASON,
                presence.getExitReasonCode(), language);

        return new PresenceResponse(
                presence.getPresenceNumber(),
                presence.getCompanyCode(),
                companyName,
                presence.getEntryReasonCode(),
                entryReasonName,
                presence.getExitReasonCode(),
                exitReasonName,
                presence.getStartDate(),
                presence.getEndDate()
        );
    }

    public List<PresenceResponse> toResponseList(String ruleSystemCode, List<Presence> presences, ResponseLanguage language) {
        return presences.stream()
                .map(presence -> toResponse(ruleSystemCode, presence, language))
                .toList();
    }

    private String label(String ruleSystemCode, String ruleEntityTypeCode, String code, ResponseLanguage language) {
        return ruleEntityLabelResolver
                .resolveName(ruleSystemCode, ruleEntityTypeCode, code, language.code())
                .orElse(null);
    }
}
