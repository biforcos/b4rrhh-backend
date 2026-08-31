package com.b4rrhh.rulesystem.company.application.usecase;

import com.b4rrhh.rulesystem.company.domain.exception.CompanyAlreadyExistsException;
import com.b4rrhh.rulesystem.company.domain.model.Company;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileCatalogValidator;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileInputNormalizer;
import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import com.b4rrhh.rulesystem.companyprofile.domain.port.CompanyProfileRepository;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.model.LiteralClass;
import com.b4rrhh.rulesystem.domain.model.MaintenanceMode;
import com.b4rrhh.rulesystem.domain.model.RuleEntityType;
import com.b4rrhh.rulesystem.domain.model.RuleSystem;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityTypeRepository;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCompanyServiceTest {

    @Mock
    private RuleEntityRepository ruleEntityRepository;
    @Mock
    private RuleSystemRepository ruleSystemRepository;
    @Mock
    private RuleEntityTypeRepository ruleEntityTypeRepository;
    @Mock
    private CompanyProfileRepository companyProfileRepository;
    private CreateCompanyService service;

    @BeforeEach
    void setUp() {
        service = new CreateCompanyService(
                ruleEntityRepository,
                ruleSystemRepository,
                ruleEntityTypeRepository,
                companyProfileRepository,
                new CompanyProfileInputNormalizer(),
                new CompanyProfileCatalogValidator(ruleEntityRepository)
        );
    }

    @Test
    void createCreatesCompanyBaseAndProfile() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(new RuleSystem(
                1L,
                "ESP",
                "Spain",
                "ESP",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        )));
        when(ruleEntityTypeRepository.findByCode("COMPANY")).thenReturn(Optional.of(new RuleEntityType(
                1L,
                "COMPANY",
                "Company",
                LiteralClass.PROPER_NOUN,
                MaintenanceMode.MAINTAINED,
                "ORGANIZATION",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        )));
        when(ruleEntityRepository.findByBusinessKey("ESP", "COMPANY", "ACME")).thenReturn(Optional.empty());
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COUNTRY", "ESP", LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.of(ruleEntity(99L, "ESP", "COUNTRY", "ESP", "Spain", LocalDate.of(1900, 1, 1), null, true)));
        when(ruleEntityRepository.save(any(RuleEntity.class))).thenReturn(ruleEntity(10L, "ESP", "COMPANY", "ACME", "Acme SA", LocalDate.of(2026, 1, 1), null, true));
        when(companyProfileRepository.save(any(Long.class), any(CompanyProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        Company result = service.create(new CreateCompanyCommand(
                "esp",
                "acme",
                "Acme SA",
                "Main company",
                LocalDate.of(2026, 1, 1),
                "Acme Spain SA",
                "A12345678",
                "Gran Via 1",
                "Madrid",
                "28013",
                "MD",
                "ESP"
        ));

        ArgumentCaptor<RuleEntity> entityCaptor = ArgumentCaptor.forClass(RuleEntity.class);
        verify(ruleEntityRepository).save(entityCaptor.capture());
        assertEquals("COMPANY", entityCaptor.getValue().getRuleEntityTypeCode());
        assertEquals("ACME", entityCaptor.getValue().getCode());

        verify(companyProfileRepository).save(any(Long.class), any(CompanyProfile.class));
        assertEquals("ESP", result.ruleSystemCode());
        assertEquals("ACME", result.companyCode());
        assertEquals("Acme Spain SA", result.legalName());
    }

    @Test
    void createReturns409WhenBusinessKeyAlreadyExists() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(new RuleSystem(
                1L,
                "ESP",
                "Spain",
                "ESP",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        )));
        when(ruleEntityTypeRepository.findByCode("COMPANY")).thenReturn(Optional.of(new RuleEntityType(
                1L,
                "COMPANY",
                "Company",
                LiteralClass.PROPER_NOUN,
                MaintenanceMode.MAINTAINED,
                "ORGANIZATION",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        )));
        when(ruleEntityRepository.findByBusinessKey("ESP", "COMPANY", "ACME"))
                .thenReturn(Optional.of(ruleEntity(10L, "ESP", "COMPANY", "ACME", "Acme SA", LocalDate.of(2026, 1, 1), null, true)));

        assertThrows(CompanyAlreadyExistsException.class, () -> service.create(new CreateCompanyCommand(
                "ESP",
                "ACME",
                "Acme SA",
                null,
                LocalDate.of(2026, 1, 1),
                "Acme Spain SA",
                null,
                null,
                null,
                null,
                null,
                null
        )));
    }

    private RuleEntity ruleEntity(
            Long id,
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            boolean active
    ) {
        return new RuleEntity(
                id,
                ruleSystemCode,
                ruleEntityTypeCode,
                code,
                name,
                null,
                active,
                startDate,
                endDate,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
