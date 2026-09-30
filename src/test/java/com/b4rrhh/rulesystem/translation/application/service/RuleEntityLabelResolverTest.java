package com.b4rrhh.rulesystem.translation.application.service;

import com.b4rrhh.rulesystem.domain.model.LiteralClass;
import com.b4rrhh.rulesystem.domain.model.MaintenanceMode;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.model.RuleEntityType;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.translation.domain.model.RuleEntityTranslation;
import com.b4rrhh.rulesystem.translation.domain.model.RuleEntityTypeTranslation;
import com.b4rrhh.rulesystem.translation.domain.port.RuleEntityTranslationRepository;
import com.b4rrhh.rulesystem.translation.domain.port.RuleEntityTypeTranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El resolutor se prueba sin HTTP: el idioma le llega como argumento (backend#23 §2).
 */
@ExtendWith(MockitoExtension.class)
class RuleEntityLabelResolverTest {

    private static final String TYPE = "EMPLOYEE_PRESENCE_ENTRY_REASON";

    @Mock
    private RuleEntityRepository ruleEntityRepository;
    @Mock
    private RuleEntityTranslationRepository translationRepository;
    @Mock
    private RuleEntityTypeTranslationRepository typeTranslationRepository;

    private RuleEntityLabelResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new RuleEntityLabelResolver(ruleEntityRepository, translationRepository, typeTranslationRepository);
    }

    @Test
    void returnsTheTranslationWhenThereIsOneForTheLanguage() {
        when(ruleEntityRepository.findByBusinessKey("ESP", TYPE, "HIRING"))
                .thenReturn(Optional.of(hiring()));
        when(translationRepository.findByRuleEntityIdAndLanguageCode(7L, "es-ES"))
                .thenReturn(Optional.of(new RuleEntityTranslation(7L, "es-ES", "Contratación", null)));

        assertThat(resolver.resolveName("ESP", TYPE, "HIRING", "es-ES")).contains("Contratación");
    }

    @Test
    void fallsBackToTheBaseLiteralWhenTheLanguageIsNotTranslated() {
        when(ruleEntityRepository.findByBusinessKey("ESP", TYPE, "HIRING"))
                .thenReturn(Optional.of(hiring()));
        when(translationRepository.findByRuleEntityIdAndLanguageCode(7L, "fr-FR"))
                .thenReturn(Optional.empty());

        assertThat(resolver.resolveName("ESP", TYPE, "HIRING", "fr-FR")).contains("Hiring");
    }

    @Test
    void returnsTheBaseLiteralWithoutLookingUpTranslationsWhenNoLanguageIsGiven() {
        when(ruleEntityRepository.findByBusinessKey("ESP", TYPE, "HIRING"))
                .thenReturn(Optional.of(hiring()));

        assertThat(resolver.resolveName("ESP", TYPE, "HIRING", null)).contains("Hiring");
        verify(translationRepository, never()).findByRuleEntityIdAndLanguageCode(anyLong(), anyString());
    }

    @Test
    void treatsALanguageWithoutCanonicalFormAsNoLanguage() {
        when(ruleEntityRepository.findByBusinessKey("ESP", TYPE, "HIRING"))
                .thenReturn(Optional.of(hiring()));

        assertThat(resolver.resolveName("ESP", TYPE, "HIRING", "not a language")).contains("Hiring");
        verify(translationRepository, never()).findByRuleEntityIdAndLanguageCode(anyLong(), anyString());
    }

    @Test
    void canonicalizesTheLanguageBeforeLookingItUp() {
        when(ruleEntityRepository.findByBusinessKey("ESP", TYPE, "HIRING"))
                .thenReturn(Optional.of(hiring()));
        when(translationRepository.findByRuleEntityIdAndLanguageCode(7L, "es-ES"))
                .thenReturn(Optional.of(new RuleEntityTranslation(7L, "es-ES", "Contratación", null)));

        assertThat(resolver.resolveName("ESP", TYPE, "HIRING", "es_es")).contains("Contratación");
    }

    @Test
    void fallsBackToTheBaseLiteralWhenTheTranslationIsBlank() {
        when(ruleEntityRepository.findByBusinessKey("ESP", TYPE, "HIRING"))
                .thenReturn(Optional.of(hiring()));
        when(translationRepository.findByRuleEntityIdAndLanguageCode(7L, "es-ES"))
                .thenReturn(Optional.of(new RuleEntityTranslation(7L, "es-ES", "   ", null)));

        assertThat(resolver.resolveName("ESP", TYPE, "HIRING", "es-ES")).contains("Hiring");
    }

    @Test
    void trimsTheLiteralAndIsEmptyWhenTheBaseLiteralIsBlank() {
        when(ruleEntityRepository.findByBusinessKey("ESP", TYPE, "HIRING"))
                .thenReturn(Optional.of(ruleEntity(7L, "  Hiring  ")));
        when(ruleEntityRepository.findByBusinessKey("ESP", TYPE, "BLANK"))
                .thenReturn(Optional.of(ruleEntity(8L, "  ")));

        assertThat(resolver.resolveName("ESP", TYPE, "HIRING", null)).contains("Hiring");
        assertThat(resolver.resolveName("ESP", TYPE, "BLANK", null)).isEmpty();
    }

    @Test
    void isEmptyWhenTheCodeDoesNotExist() {
        when(ruleEntityRepository.findByBusinessKey("ESP", TYPE, "UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThat(resolver.resolveName("ESP", TYPE, "UNKNOWN", "es-ES")).isEmpty();
    }

    @Test
    void normalizesRuleSystemAndCodeToUppercaseAndIsEmptyWhenEitherIsMissing() {
        when(ruleEntityRepository.findByBusinessKey("ESP", TYPE, "HIRING"))
                .thenReturn(Optional.of(hiring()));

        assertThat(resolver.resolveName(" esp ", TYPE, " hiring ", null)).contains("Hiring");
        assertThat(resolver.resolveName("ESP", TYPE, null, null)).isEmpty();
        assertThat(resolver.resolveName(null, TYPE, "HIRING", null)).isEmpty();
        assertThat(resolver.resolveName("ESP", TYPE, "  ", null)).isEmpty();
    }

    // backend#152: la lista de Catálogos resuelve todas sus filas con una sola consulta, y
    // la que no tiene traducción cae a su literal base, como resolveName.
    @Test
    void resolvesTheLabelOfEveryEntityInOneLookupAndFallsBackToItsBaseLiteral() {
        RuleEntity retirement = ruleEntity(8L, "Retirement");
        when(translationRepository.findByRuleEntityIdsAndLanguageCode(List.of(7L, 8L), "es-ES"))
                .thenReturn(List.of(new RuleEntityTranslation(7L, "es-ES", "Contratación", null)));

        Map<Long, String> labels = resolver.resolveLabels(List.of(hiring(), retirement), "es_es");

        assertThat(labels).containsExactlyInAnyOrderEntriesOf(Map.of(7L, "Contratación", 8L, "Retirement"));
        verify(translationRepository, never()).findByRuleEntityIdAndLanguageCode(anyLong(), anyString());
    }

    @Test
    void entityLabelsAreTheBaseLiteralsWithoutLookingUpTranslationsWhenNoLanguageIsGiven() {
        assertThat(resolver.resolveLabels(List.of(hiring()), null))
                .containsExactlyEntriesOf(Map.of(7L, "Hiring"));
        verify(translationRepository, never()).findByRuleEntityIdsAndLanguageCode(any(), anyString());
    }

    // backend#152: el nombre de un tipo se traduce como el de una entidad, y una lista de
    // tipos se resuelve con una sola consulta, no una por tipo.
    @Test
    void resolvesTheLabelOfEveryTypeAndFallsBackToItsStoredName() {
        when(typeTranslationRepository.findByLanguageCode("es-ES"))
                .thenReturn(List.of(new RuleEntityTypeTranslation("CONTACT_TYPE", "es-ES", "Tipo de contacto")));

        Map<String, String> labels = resolver.resolveTypeLabels(
                List.of(type("CONTACT_TYPE", "Contact Type"), type("COUNTRY", "Country")), "es_es");

        assertThat(labels).containsExactlyInAnyOrderEntriesOf(Map.of(
                "CONTACT_TYPE", "Tipo de contacto",
                "COUNTRY", "Country"));
    }

    @Test
    void typeLabelsAreTheStoredNamesWithoutLookingUpTranslationsWhenNoLanguageIsGiven() {
        Map<String, String> labels = resolver.resolveTypeLabels(
                List.of(type("CONTACT_TYPE", "Contact Type")), null);

        assertThat(labels).containsExactlyEntriesOf(Map.of("CONTACT_TYPE", "Contact Type"));
        verify(typeTranslationRepository, never()).findByLanguageCode(anyString());
    }

    @Test
    void aBlankTypeTranslationFallsBackToTheStoredName() {
        when(typeTranslationRepository.findByLanguageCode("es-ES"))
                .thenReturn(List.of(new RuleEntityTypeTranslation("CONTACT_TYPE", "es-ES", "  ")));

        assertThat(resolver.resolveTypeLabels(List.of(type("CONTACT_TYPE", "Contact Type")), "es-ES"))
                .containsExactlyEntriesOf(Map.of("CONTACT_TYPE", "Contact Type"));
    }

    private static RuleEntityType type(String code, String name) {
        return new RuleEntityType(
                1L, code, name, LiteralClass.DOMAIN_VOCABULARY, MaintenanceMode.MAINTAINED,
                "ORGANIZATION", true, LocalDateTime.now(), LocalDateTime.now());
    }

    private static RuleEntity hiring() {
        return ruleEntity(7L, "Hiring");
    }

    private static RuleEntity ruleEntity(Long id, String name) {
        return new RuleEntity(
                id,
                "ESP",
                TYPE,
                "HIRING",
                name,
                null,
                true,
                LocalDate.of(1900, 1, 1),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
