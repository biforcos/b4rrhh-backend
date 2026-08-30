package com.b4rrhh.rulesystem.translation.infrastructure.web.dto;

import com.b4rrhh.rulesystem.translation.application.usecase.RuleEntityTranslationCoverage;

import java.util.List;

public record RuleEntityTranslationCoverageResponse(String languageCode, List<TypeCoverageResponse> types) {

    public static RuleEntityTranslationCoverageResponse from(RuleEntityTranslationCoverage coverage) {
        return new RuleEntityTranslationCoverageResponse(
                coverage.languageCode(),
                coverage.types().stream()
                        .map(type -> new TypeCoverageResponse(
                                type.ruleEntityTypeCode(),
                                type.total(),
                                type.translated(),
                                type.missing(),
                                type.missingCodes().stream()
                                        .map(code -> new MissingCodeResponse(
                                                code.ruleSystemCode(), code.code(), code.name()))
                                        .toList()
                        ))
                        .toList()
        );
    }

    public record TypeCoverageResponse(
            String ruleEntityTypeCode,
            long total,
            long translated,
            long missing,
            List<MissingCodeResponse> missingCodes
    ) {
    }

    public record MissingCodeResponse(String ruleSystemCode, String code, String name) {
    }
}
