package com.b4rrhh.rulesystem.translation.domain.port;

import com.b4rrhh.rulesystem.translation.domain.model.RuleEntityTypeTranslation;

import java.util.List;

public interface RuleEntityTypeTranslationRepository {

    /** Todas las traducciones de un idioma: son pocas —una por tipo— y se piden de una vez. */
    List<RuleEntityTypeTranslation> findByLanguageCode(String languageCode);
}
