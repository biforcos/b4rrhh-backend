package com.b4rrhh.rulesystem.translation.application.usecase;

import com.b4rrhh.rulesystem.translation.application.port.RuleEntityTranslationCoverageReadPort;
import com.b4rrhh.rulesystem.translation.application.port.RuleEntityTranslationCoverageReadPort.UntranslatedRuleEntity;
import com.b4rrhh.rulesystem.translation.application.usecase.RuleEntityTranslationCoverage.MissingCode;
import com.b4rrhh.rulesystem.translation.application.usecase.RuleEntityTranslationCoverage.TypeCoverage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRuleEntityTranslationCoverageServiceTest {

    @Mock
    private RuleEntityTranslationCoverageReadPort coverageReadPort;

    @Test
    void countsTranslatedAndMissingPerTypeAndListsTheMissingCodes() {
        when(coverageReadPort.countRuleEntitiesByType()).thenReturn(Map.of(
                "EMPLOYEE_PRESENCE_ENTRY_REASON", 3L,
                "EMPLOYEE_ADDRESS_TYPE", 2L
        ));
        when(coverageReadPort.findUntranslated("es-ES")).thenReturn(List.of(
                new UntranslatedRuleEntity("EMPLOYEE_PRESENCE_ENTRY_REASON", "FRA", "HIRING", "Hiring"),
                new UntranslatedRuleEntity("EMPLOYEE_PRESENCE_ENTRY_REASON", "PRT", "HIRING", "Hiring")
        ));

        RuleEntityTranslationCoverage coverage =
                new GetRuleEntityTranslationCoverageService(coverageReadPort).getCoverage("es-ES");

        assertThat(coverage.languageCode()).isEqualTo("es-ES");
        assertThat(coverage.types()).containsExactly(
                new TypeCoverage("EMPLOYEE_ADDRESS_TYPE", 2, 2, 0, List.of()),
                new TypeCoverage("EMPLOYEE_PRESENCE_ENTRY_REASON", 3, 1, 2, List.of(
                        new MissingCode("FRA", "HIRING", "Hiring"),
                        new MissingCode("PRT", "HIRING", "Hiring")
                ))
        );
    }

    @Test
    void everyTypeAppearsEvenWhenNothingIsTranslated() {
        when(coverageReadPort.countRuleEntitiesByType()).thenReturn(Map.of("CONTACT_TYPE", 1L));
        when(coverageReadPort.findUntranslated("fr-FR")).thenReturn(List.of(
                new UntranslatedRuleEntity("CONTACT_TYPE", "ESP", "EMAIL", "Email")
        ));

        RuleEntityTranslationCoverage coverage =
                new GetRuleEntityTranslationCoverageService(coverageReadPort).getCoverage("fr-FR");

        assertThat(coverage.types()).singleElement().satisfies(type -> {
            assertThat(type.translated()).isZero();
            assertThat(type.missing()).isEqualTo(1);
        });
    }
}
