package com.b4rrhh.rulesystem.translation.application.usecase;

public interface GetRuleEntityTranslationCoverageUseCase {

    /**
     * @param languageCode idioma en BCP 47 corto, ya canónico ({@code es-ES})
     */
    RuleEntityTranslationCoverage getCoverage(String languageCode);
}
