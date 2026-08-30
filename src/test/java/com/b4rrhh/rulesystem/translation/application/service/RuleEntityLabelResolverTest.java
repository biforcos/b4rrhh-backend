package com.b4rrhh.rulesystem.translation.application.service;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.translation.domain.model.RuleEntityTranslation;
import com.b4rrhh.rulesystem.translation.domain.port.RuleEntityTranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    private RuleEntityLabelResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new RuleEntityLabelResolver(ruleEntityRepository, translationRepository);
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
