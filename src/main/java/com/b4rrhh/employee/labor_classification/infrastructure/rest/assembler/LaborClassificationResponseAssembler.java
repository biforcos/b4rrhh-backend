package com.b4rrhh.employee.labor_classification.infrastructure.rest.assembler;

import com.b4rrhh.employee.labor_classification.application.usecase.LaborClassificationRuleEntityTypeCodes;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.infrastructure.rest.dto.LaborClassificationResponse;
import com.b4rrhh.rulesystem.agreementcategoryprofile.domain.port.AgreementCategoryProfileRepository;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class LaborClassificationResponseAssembler {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;
    private final AgreementCategoryProfileRepository agreementCategoryProfileRepository;

    public LaborClassificationResponseAssembler(
            RuleEntityLabelResolver ruleEntityLabelResolver,
            AgreementCategoryProfileRepository agreementCategoryProfileRepository
    ) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
        this.agreementCategoryProfileRepository = agreementCategoryProfileRepository;
    }

    public LaborClassificationResponse toResponse(
            String ruleSystemCode,
            LaborClassification laborClassification,
            ResponseLanguage language
    ) {
        String agreementName = ruleEntityLabelResolver
                .resolveName(ruleSystemCode, LaborClassificationRuleEntityTypeCodes.AGREEMENT,
                        laborClassification.getAgreementCode(), language.code())
                .orElse(null);
        String agreementCategoryName = ruleEntityLabelResolver
                .resolveName(ruleSystemCode, LaborClassificationRuleEntityTypeCodes.AGREEMENT_CATEGORY,
                        laborClassification.getAgreementCategoryCode(), language.code())
                .orElse(null);
        String grupoCotizacionCode = findGrupoCotizacionCode(ruleSystemCode, laborClassification.getAgreementCategoryCode())
                .orElse(null);
        // Cita reglamentaria (ADR-054): el resolutor devuelve el literal base porque no hay
        // traduccion que sembrar. Sin grupo, sin nombre: nada de relleno (backend#41).
        String grupoCotizacionName = grupoCotizacionCode == null
                ? null
                : ruleEntityLabelResolver
                        .resolveName(ruleSystemCode, LaborClassificationRuleEntityTypeCodes.GRUPO_COTIZACION,
                                grupoCotizacionCode, language.code())
                        .orElse(null);

        return new LaborClassificationResponse(
                laborClassification.getAgreementCode(),
                agreementName,
                laborClassification.getAgreementCategoryCode(),
                agreementCategoryName,
                grupoCotizacionCode,
                grupoCotizacionName,
                laborClassification.getStartDate(),
                laborClassification.getEndDate()
        );
    }

    public List<LaborClassificationResponse> toResponseList(
            String ruleSystemCode,
            List<LaborClassification> laborClassifications,
            ResponseLanguage language
    ) {
        return laborClassifications.stream()
                .map(laborClassification -> toResponse(ruleSystemCode, laborClassification, language))
                .toList();
    }

    // El grupo de cotizacion no es un literal: es un dato del perfil de la categoria. Misma
    // normalizacion que hacia el adaptador que vivia aqui antes.
    private Optional<String> findGrupoCotizacionCode(String ruleSystemCode, String agreementCategoryCode) {
        if (agreementCategoryCode == null || agreementCategoryCode.trim().isEmpty()) {
            return Optional.empty();
        }
        return agreementCategoryProfileRepository.findGrupoCotizacionCodeByCategoryCode(
                ruleSystemCode.trim().toUpperCase(Locale.ROOT),
                agreementCategoryCode.trim().toUpperCase(Locale.ROOT)
        );
    }
}
