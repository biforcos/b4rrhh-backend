package com.b4rrhh.rulesystem.company.application.usecase;

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
class ListCompaniesServiceTest {

    @Mock
    private RuleEntityRepository ruleEntityRepository;
    @Mock
    private CompanyProfileRepository companyProfileRepository;

    private ListCompaniesService service;

    @BeforeEach
    void setUp() {
        service = new ListCompaniesService(ruleEntityRepository, companyProfileRepository, new CompanyProfileInputNormalizer());
    }

    @Test
    void listReturnsAggregatedCompanies() {
        RuleEntity company = new RuleEntity(
                10L,
                "ESP",
                "COMPANY",
                "ACME",
                "Acme",
                null,
                true,
                LocalDate.of(2020, 1, 1),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(ruleEntityRepository.findByFilters(null, "COMPANY", null, true, LocalDate.now()))
                .thenReturn(List.of(company));
        when(companyProfileRepository.findByCompanyRuleEntityId(10L)).thenReturn(Optional.of(
                new CompanyProfile("Acme Spain SA", "A123", null, null, null, null, "ESP", null)
        ));

        var result = service.list(new ListCompaniesQuery(null));

        assertEquals(1, result.size());
        assertEquals("ACME", result.getFirst().companyCode());
        assertEquals("Acme Spain SA", result.getFirst().legalName());
    }

    // backend#34: una empresa sin perfil es una inconsistencia de datos, no un caso que se
    // tapa con el nombre de la raiz. Si esto vuelve a devolver algo en vez de fallar, se ha
    // reintroducido el fallback.
    @Test
    void listFailsWhenACompanyHasNoProfile() {
        RuleEntity company = new RuleEntity(
                10L,
                "ESP",
                "COMPANY",
                "ACME",
                "Acme",
                null,
                true,
                LocalDate.of(2020, 1, 1),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(ruleEntityRepository.findByFilters(null, "COMPANY", null, true, LocalDate.now()))
                .thenReturn(List.of(company));
        when(companyProfileRepository.findByCompanyRuleEntityId(10L)).thenReturn(Optional.empty());

        assertThrows(RequiredExtensionMissingException.class, () -> service.list(new ListCompaniesQuery(null)));
    }
}
