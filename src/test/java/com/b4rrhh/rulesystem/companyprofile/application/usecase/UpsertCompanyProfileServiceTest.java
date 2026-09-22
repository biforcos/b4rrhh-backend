package com.b4rrhh.rulesystem.companyprofile.application.usecase;

import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileCatalogValidator;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileCompanyResolver;
import com.b4rrhh.rulesystem.companyprofile.application.service.CompanyProfileInputNormalizer;
import com.b4rrhh.rulesystem.companyprofile.domain.exception.CompanyProfileCompanyNotApplicableException;
import com.b4rrhh.rulesystem.companyprofile.domain.exception.CompanyProfileCompanyNotFoundException;
import com.b4rrhh.rulesystem.companyprofile.domain.exception.CompanyProfileCountryInvalidException;
import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import com.b4rrhh.rulesystem.companyprofile.domain.port.CompanyProfileRepository;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
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
class UpsertCompanyProfileServiceTest {

    @Mock
    private CompanyProfileRepository companyProfileRepository;
    @Mock
    private RuleEntityRepository ruleEntityRepository;

    private UpsertCompanyProfileService service;

    @BeforeEach
    void setUp() {
        service = new UpsertCompanyProfileService(
                companyProfileRepository,
                                new CompanyProfileCompanyResolver(ruleEntityRepository),
                                new CompanyProfileInputNormalizer(),
                new CompanyProfileCatalogValidator(ruleEntityRepository)
        );
    }

    @Test
    void createsProfileWhenItDoesNotExist() {
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", java.time.LocalDate.now()))
                .thenReturn(Optional.of(ruleEntity(10L, "ESP", "COMPANY", "ACME")));
        when(companyProfileRepository.findByCompanyRuleEntityId(10L)).thenReturn(Optional.empty());
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COUNTRY", "ESP", java.time.LocalDate.now()))
                .thenReturn(Optional.of(ruleEntity(99L, "ESP", "COUNTRY", "ESP")));
        when(companyProfileRepository.save(any(Long.class), any(CompanyProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        CompanyProfile result = service.upsert(new UpsertCompanyProfileCommand(
                "esp",
                "acme",
                "Acme Spain SA",
                "A12345678",
                "Gran Via 1",
                "Madrid",
                "28013",
                "MD",
                "esp",
                null
        ));

        ArgumentCaptor<CompanyProfile> captor = ArgumentCaptor.forClass(CompanyProfile.class);
        verify(companyProfileRepository).save(org.mockito.Mockito.eq(10L), captor.capture());
        assertEquals("Acme Spain SA", captor.getValue().getLegalName());
        assertEquals("ESP", captor.getValue().getCountryCode());
        assertEquals("Acme Spain SA", result.getLegalName());
    }

    @Test
    void updatesExistingProfileWhenItAlreadyExists() {
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", java.time.LocalDate.now()))
                .thenReturn(Optional.of(ruleEntity(10L, "ESP", "COMPANY", "ACME")));
        when(companyProfileRepository.findByCompanyRuleEntityId(10L))
                .thenReturn(Optional.of(new CompanyProfile(
                        "Old Legal Name",
                        null,
                        "Old Street",
                        "Madrid",
                        null,
                        null,
                        null,
                        null
                )));
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COUNTRY", "ESP", java.time.LocalDate.now()))
                .thenReturn(Optional.of(ruleEntity(99L, "ESP", "COUNTRY", "ESP")));
        when(companyProfileRepository.save(any(Long.class), any(CompanyProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        CompanyProfile result = service.upsert(new UpsertCompanyProfileCommand(
                "ESP",
                "ACME",
                "Acme Spain SA",
                "A12345678",
                "Gran Via 1",
                "Madrid",
                "28013",
                "MD",
                "ESP",
                null
        ));

        assertEquals("Acme Spain SA", result.getLegalName());
        assertEquals("Gran Via 1", result.getStreet());
        assertEquals("ESP", result.getCountryCode());
    }

    @Test
    void throwsNotFoundWhenCompanyDoesNotExist() {
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "MISSING", java.time.LocalDate.now()))
                .thenReturn(Optional.empty());
        when(ruleEntityRepository.findByFilters("ESP", "COMPANY", "MISSING", null, null))
                .thenReturn(java.util.List.of());

        assertThrows(
                CompanyProfileCompanyNotFoundException.class,
                () -> service.upsert(new UpsertCompanyProfileCommand(
                        "ESP",
                        "MISSING",
                        "Missing Company SA",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ))
        );
    }

    @Test
    void rejectsInvalidCountryCodeWhenProvided() {
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", java.time.LocalDate.now()))
                .thenReturn(Optional.of(ruleEntity(10L, "ESP", "COMPANY", "ACME")));
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COUNTRY", "ZZZ", java.time.LocalDate.now()))
                .thenReturn(Optional.empty());

        assertThrows(
                CompanyProfileCountryInvalidException.class,
                () -> service.upsert(new UpsertCompanyProfileCommand(
                        "ESP",
                        "ACME",
                        "Acme Spain SA",
                        null,
                        null,
                        null,
                        null,
                        null,
                        "ZZZ",
                        null
                ))
        );
    }

    @Test
    void rejectsUpsertWhenCompanyIsNotApplicableToday() {
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", java.time.LocalDate.now()))
                .thenReturn(Optional.empty());
        when(ruleEntityRepository.findByFilters("ESP", "COMPANY", "ACME", null, null))
                .thenReturn(java.util.List.of(ruleEntity(10L, "ESP", "COMPANY", "ACME")));

        assertThrows(
                CompanyProfileCompanyNotApplicableException.class,
                () -> service.upsert(new UpsertCompanyProfileCommand(
                        "ESP",
                        "ACME",
                        "Acme Spain SA",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ))
        );
    }

    @Test
    void storesCnaeCodeWhenProvided() {
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", LocalDate.now()))
                .thenReturn(Optional.of(ruleEntity(10L, "ESP", "COMPANY", "ACME")));
        when(companyProfileRepository.findByCompanyRuleEntityId(10L)).thenReturn(Optional.empty());
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COUNTRY", "ESP", LocalDate.now()))
                .thenReturn(Optional.of(ruleEntity(99L, "ESP", "COUNTRY", "ESP")));
        when(companyProfileRepository.save(any(Long.class), any(CompanyProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        CompanyProfile result = service.upsert(new UpsertCompanyProfileCommand(
                "ESP", "ACME", "Acme Spain SA", null,
                null, null, null, null, "ESP", "6210"
        ));

        assertEquals("6210", result.getCnaeCode());
    }

    private RuleEntity ruleEntity(Long id, String ruleSystemCode, String ruleEntityTypeCode, String code) {
        return new RuleEntity(
                id,
                ruleSystemCode,
                ruleEntityTypeCode,
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
}