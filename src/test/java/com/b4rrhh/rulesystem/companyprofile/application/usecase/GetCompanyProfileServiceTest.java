package com.b4rrhh.rulesystem.companyprofile.application.usecase;

import com.b4rrhh.support.DireccionesDePrueba;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileCompanyResolver;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileInputNormalizer;
import com.b4rrhh.rulesystem.companyprofile.domain.exception.CompanyProfileCompanyNotApplicableException;
import com.b4rrhh.rulesystem.companyprofile.domain.exception.CompanyProfileCompanyNotFoundException;
import com.b4rrhh.rulesystem.companyprofile.domain.exception.CompanyProfileNotFoundException;
import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import com.b4rrhh.rulesystem.companyprofile.domain.port.CompanyProfileRepository;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCompanyProfileServiceTest {

    @Mock
    private CompanyProfileRepository companyProfileRepository;
    @Mock
    private RuleEntityRepository ruleEntityRepository;

    private GetCompanyProfileService service;

    @BeforeEach
    void setUp() {
        service = new GetCompanyProfileService(
                companyProfileRepository,
                                new CompanyProfileCompanyResolver(ruleEntityRepository),
                                new CompanyProfileInputNormalizer()
        );
    }

    @Test
    void returnsProfileWhenCompanyAndProfileExist() {
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", java.time.LocalDate.now()))
                .thenReturn(Optional.of(companyRuleEntity(20L, "ESP", "ACME")));
        when(companyProfileRepository.findByCompanyRuleEntityId(20L))
                .thenReturn(Optional.of(companyProfile("Acme Spain SA")));

        CompanyProfile result = service.get(new GetCompanyProfileQuery("esp", "acme"));

        assertEquals("Acme Spain SA", result.getLegalName());
    }

    @Test
    void throwsNotFoundWhenProfileDoesNotExist() {
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", java.time.LocalDate.now()))
                .thenReturn(Optional.of(companyRuleEntity(20L, "ESP", "ACME")));
        when(companyProfileRepository.findByCompanyRuleEntityId(20L))
                .thenReturn(Optional.empty());

        assertThrows(
                CompanyProfileNotFoundException.class,
                () -> service.get(new GetCompanyProfileQuery("ESP", "ACME"))
        );
    }

    @Test
    void throwsNotFoundWhenCompanyDoesNotExist() {
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "MISSING", java.time.LocalDate.now()))
                .thenReturn(Optional.empty());
        when(ruleEntityRepository.findByFilters("ESP", "COMPANY", "MISSING", null, null))
                .thenReturn(java.util.List.of());

        assertThrows(
                CompanyProfileCompanyNotFoundException.class,
                () -> service.get(new GetCompanyProfileQuery("ESP", "missing"))
        );
    }

    @Test
    void throwsNotFoundWhenCompanyIsNotApplicableToday() {
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", java.time.LocalDate.now()))
                .thenReturn(Optional.empty());
        when(ruleEntityRepository.findByFilters("ESP", "COMPANY", "ACME", null, null))
                .thenReturn(java.util.List.of(companyRuleEntity(20L, "ESP", "ACME")));

        assertThrows(
                CompanyProfileCompanyNotApplicableException.class,
                () -> service.get(new GetCompanyProfileQuery("ESP", "ACME"))
        );
    }

    private RuleEntity companyRuleEntity(Long id, String ruleSystemCode, String code) {
        return new RuleEntity(
                id,
                ruleSystemCode,
                "COMPANY",
                code,
                code,
                null,
                true,
                LocalDate.of(1900, 1, 1),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private CompanyProfile companyProfile(String legalName) {
        return new CompanyProfile(legalName, "A12345678", DireccionesDePrueba.address("Gran Via 1", "Madrid", "ESP", "28013"), null);
    }
}