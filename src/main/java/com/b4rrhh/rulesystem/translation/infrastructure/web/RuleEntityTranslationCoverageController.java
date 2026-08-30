package com.b4rrhh.rulesystem.translation.infrastructure.web;

import com.b4rrhh.rulesystem.translation.application.usecase.GetRuleEntityTranslationCoverageUseCase;
import com.b4rrhh.rulesystem.translation.domain.model.LanguageCode;
import com.b4rrhh.rulesystem.translation.infrastructure.web.dto.RuleEntityTranslationCoverageResponse;
import com.b4rrhh.rulesystem.translation.infrastructure.web.dto.RuleEntityTranslationErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * El informe de cobertura de traducciones (ADR-052 §5). Es de administración: no hay
 * pantalla, con que se pueda pedir y leer vale.
 */
@RestController
@RequestMapping("/rule-entity-translations")
public class RuleEntityTranslationCoverageController {

    private final GetRuleEntityTranslationCoverageUseCase getCoverageUseCase;

    public RuleEntityTranslationCoverageController(GetRuleEntityTranslationCoverageUseCase getCoverageUseCase) {
        this.getCoverageUseCase = getCoverageUseCase;
    }

    @GetMapping("/coverage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> coverage(@RequestParam String languageCode) {
        return LanguageCode.canonical(languageCode)
                .<ResponseEntity<?>>map(language -> ResponseEntity.ok(
                        RuleEntityTranslationCoverageResponse.from(getCoverageUseCase.getCoverage(language))))
                .orElseGet(() -> ResponseEntity.badRequest().body(new RuleEntityTranslationErrorResponse(
                        "INVALID_LANGUAGE_CODE",
                        "languageCode must be a short BCP 47 tag such as es-ES, fr-FR or en: " + languageCode
                )));
    }
}
