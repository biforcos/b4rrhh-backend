package com.b4rrhh.rulesystem.translation.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageCodeTest {

    @ParameterizedTest
    @CsvSource({
            "es-ES, es-ES",
            "es_ES, es-ES",
            "ES-es, es-ES",
            "es-es, es-ES",
            "' es-ES ', es-ES",
            "en, en",
            "EN, en",
            "fr-FR, fr-FR"
    })
    void canonicalizesEveryWayOfWritingTheSameLanguage(String raw, String expected) {
        assertThat(LanguageCode.canonical(raw)).contains(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"*", "spanish", "e", "es-ESP", "es-E1", "es-ES-x-private", "123", "not a language"})
    void rejectsWhatDoesNotLookLikeAShortBcp47Tag(String raw) {
        assertThat(LanguageCode.canonical(raw)).isEmpty();
    }
}
