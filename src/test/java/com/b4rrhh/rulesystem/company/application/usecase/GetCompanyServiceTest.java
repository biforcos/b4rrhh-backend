package com.b4rrhh.rulesystem.company.application.usecase;

import com.b4rrhh.rulesystem.company.domain.exception.CompanyNotApplicableException;
import com.b4rrhh.rulesystem.company.domain.exception.CompanyNotFoundException;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileCompanyResolver;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileInputNormalizer;
import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import com.b4rrhh.rulesystem.companyprofile.domain.port.CompanyProfileRepository;
import com.b4rrhh.rulesystem.domain.exception.RequiredExtensionMissingException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCompanyServiceTest {

    @Mock
    private RuleEntityRepository ruleEntityRepository;
    @Mock
    private CompanyProfileRepository companyProfileRepository;

    private GetCompanyService service;

    @BeforeEach
    void setUp() {
        service = new GetCompanyService(
                new CompanyProfileCompanyResolver(ruleEntityRepository),
                new CompanyProfileInputNormalizer(),
                companyProfileRepository
        );
    }

    @Test
    void getUsesResolverAndReturnsAggregatedCompany() {
        RuleEntity company = ruleEntity();
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", LocalDate.now()))
                .thenReturn(Optional.of(company));
        when(companyProfileRepository.findByCompanyRuleEntityId(10L)).thenReturn(Optional.of(
                new CompanyProfile("Acme Spain SA", "A123", "Gran Via 1", "Madrid", "28013", "MD", "ESP", null)
        ));

        var result = service.get(new GetCompanyQuery("esp", "acme"));

        assertEquals("ESP", result.ruleSystemCode());
        assertEquals("ACME", result.companyCode());
        assertEquals("Acme Spain SA", result.legalName());
    }

    // backend#34: antes este caso fabricaba un perfil de aire; ahora es una inconsistencia
    // de datos y falla. Si esto vuelve a responder, se ha reintroducido el fallback.
    @Test
    void getFailsWhenProfileMissing() {
        RuleEntity company = ruleEntity();
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", LocalDate.now()))
                .thenReturn(Optional.of(company));
        when(companyProfileRepository.findByCompanyRuleEntityId(10L)).thenReturn(Optional.empty());

        assertThrows(RequiredExtensionMissingException.class, () -> service.get(new GetCompanyQuery("esp", "acme")));
    }

    @Test
    void getMapsResolverNotFoundToCompanyNotFound() {
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", LocalDate.now()))
                .thenReturn(Optional.empty());
        when(ruleEntityRepository.findByFilters("ESP", "COMPANY", "ACME", null, null))
                .thenReturn(List.of());

        assertThrows(CompanyNotFoundException.class, () -> service.get(new GetCompanyQuery("ESP", "ACME")));
    }

    @Test
    void getMapsResolverNotApplicableToConflictException() {
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", LocalDate.now()))
                .thenReturn(Optional.empty());
        when(ruleEntityRepository.findByFilters("ESP", "COMPANY", "ACME", null, null))
                .thenReturn(List.of(ruleEntity()));

        assertThrows(CompanyNotApplicableException.class, () -> service.get(new GetCompanyQuery("ESP", "ACME")));
    }

    private RuleEntity ruleEntity() {
        return new RuleEntity(
                10L,
                "ESP",
                "COMPANY",
                "ACME",
                "Acme SA",
                "Main company",
                true,
                LocalDate.of(2020, 1, 1),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
