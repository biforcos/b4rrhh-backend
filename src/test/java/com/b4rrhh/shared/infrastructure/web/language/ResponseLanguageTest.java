package com.b4rrhh.shared.infrastructure.web.language;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseLanguageTest {

    @ParameterizedTest
    @CsvSource({
            "es-ES, es-ES",
            "'es-ES,es;q=0.9,en;q=0.8', es-ES",
            "'en;q=0.5,fr-FR;q=0.9', fr-FR",
            "es, es",
            "ES-es, es-ES",
            "'*,es-ES;q=0.5', es-ES",
            "'es-ES-u-co-trad', es-ES"
    })
    void takesTheHeaviestLanguageThatHasAShortCanonicalForm(String header, String expected) {
        assertThat(ResponseLanguage.fromAcceptLanguage(header).code()).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "*", "not a language at all", "es_ES", ";;;", "xx-YY-ZZZZZZZZZ-extra-stuff-q=oops"})
    void fallsBackToTheBaseLiteralWithoutAHeaderOrWithOneThatIsNotALanguage(String header) {
        assertThat(ResponseLanguage.fromAcceptLanguage(header).code()).isNull();
    }
}
