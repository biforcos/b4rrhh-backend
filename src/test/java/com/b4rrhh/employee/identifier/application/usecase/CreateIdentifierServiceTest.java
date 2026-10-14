package com.b4rrhh.employee.identifier.application.usecase;

import com.b4rrhh.employee.identifier.application.port.EmployeeIdentifierContext;
import com.b4rrhh.employee.identifier.application.port.EmployeeIdentifierLookupPort;
import com.b4rrhh.employee.identifier.application.service.IdentifierCatalogValidator;
import com.b4rrhh.employee.identifier.application.service.SpanishNationalIdValidator;
import com.b4rrhh.employee.identifier.domain.exception.IdentifierAlreadyExistsException;
import com.b4rrhh.employee.identifier.domain.exception.IdentifierCatalogValueInvalidException;
import com.b4rrhh.employee.identifier.domain.exception.IdentifierPrimaryAlreadyExistsException;
import com.b4rrhh.employee.identifier.domain.exception.IdentifierSpanishNationalIdInvalidException;
import com.b4rrhh.employee.identifier.domain.exception.IdentifierValueInvalidException;
import com.b4rrhh.employee.identifier.domain.model.Identifier;
import com.b4rrhh.employee.identifier.domain.port.IdentifierRepository;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.model.RuleSystem;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateIdentifierServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";

    @Mock
    private IdentifierRepository identifierRepository;
    @Mock
    private EmployeeIdentifierLookupPort employeeIdentifierLookupPort;
    @Mock
    private RuleSystemRepository ruleSystemRepository;
    @Mock
    private RuleEntityRepository ruleEntityRepository;

    private CreateIdentifierService service;

    @BeforeEach
    void setUp() {
        IdentifierCatalogValidator identifierCatalogValidator = new IdentifierCatalogValidator(ruleEntityRepository);
        SpanishNationalIdValidator spanishNationalIdValidator = new SpanishNationalIdValidator();
        service = new CreateIdentifierService(
                identifierRepository,
                employeeIdentifierLookupPort,
                ruleSystemRepository,
                identifierCatalogValidator,
                spanishNationalIdValidator
        );
    }

    @Test
    void createsValidIdentifier() {
        CreateIdentifierCommand command = new CreateIdentifierCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                "EMP001",
                "national_id",
                "12345678Z",
                "esp",
                LocalDate.of(2030, 12, 31),
                true
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeIdentifierLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, "EMP001"))
                .thenReturn(Optional.of(employeeContext(10L, "EMP001")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "EMPLOYEE_IDENTIFIER_TYPE", "NATIONAL_ID"))
                .thenReturn(Optional.of(activeIdentifierTypeRuleEntity(RULE_SYSTEM_CODE, "NATIONAL_ID")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "COUNTRY", "ESP"))
                .thenReturn(Optional.of(activeCountryRuleEntity(RULE_SYSTEM_CODE, "ESP")));
        when(identifierRepository.findByEmployeeIdAndIdentifierTypeCode(10L, "NATIONAL_ID")).thenReturn(Optional.empty());
        when(identifierRepository.existsByEmployeeIdAndIsPrimaryTrue(10L)).thenReturn(false);
        when(identifierRepository.save(any(Identifier.class))).thenAnswer(invocation -> {
            Identifier input = invocation.getArgument(0);
            return new Identifier(
                    99L,
                    input.getEmployeeId(),
                    input.getIdentifierTypeCode(),
                    input.getIdentifierValue(),
                    input.getIssuingCountryCode(),
                    input.getExpirationDate(),
                    input.isPrimary(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
        });

        Identifier created = service.create(command);

        assertEquals(99L, created.getId());
        assertEquals("NATIONAL_ID", created.getIdentifierTypeCode());
        assertEquals("12345678Z", created.getIdentifierValue());
        assertEquals("ESP", created.getIssuingCountryCode());
        assertEquals(true, created.isPrimary());

        ArgumentCaptor<Identifier> captor = ArgumentCaptor.forClass(Identifier.class);
        verify(identifierRepository).save(captor.capture());
        assertEquals("NATIONAL_ID", captor.getValue().getIdentifierTypeCode());
    }

    @Test
    void rejectsCreateWhenIdentifierTypeIsInvalid() {
        CreateIdentifierCommand command = new CreateIdentifierCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                "EMP001",
                "UNKNOWN",
                "12345678A",
                null,
                null,
                false
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeIdentifierLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, "EMP001"))
                .thenReturn(Optional.of(employeeContext(10L, "EMP001")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "EMPLOYEE_IDENTIFIER_TYPE", "UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThrows(IdentifierCatalogValueInvalidException.class, () -> service.create(command));
        verify(identifierRepository, never()).save(any(Identifier.class));
    }

    @Test
    void rejectsCreateWhenIssuingCountryCodeIsInvalid() {
        CreateIdentifierCommand command = new CreateIdentifierCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                "EMP001",
                "NATIONAL_ID",
                "12345678A",
                "ZZZ",
                null,
                false
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeIdentifierLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, "EMP001"))
                .thenReturn(Optional.of(employeeContext(10L, "EMP001")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "EMPLOYEE_IDENTIFIER_TYPE", "NATIONAL_ID"))
                .thenReturn(Optional.of(activeIdentifierTypeRuleEntity(RULE_SYSTEM_CODE, "NATIONAL_ID")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "COUNTRY", "ZZZ"))
                .thenReturn(Optional.empty());

        assertThrows(IdentifierCatalogValueInvalidException.class, () -> service.create(command));
        verify(identifierRepository, never()).save(any(Identifier.class));
    }

    @Test
    void rejectsCreateWhenIdentifierValueIsInvalid() {
        CreateIdentifierCommand command = new CreateIdentifierCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                "EMP001",
                "NATIONAL_ID",
                "  ",
                null,
                null,
                false
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeIdentifierLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, "EMP001"))
                .thenReturn(Optional.of(employeeContext(10L, "EMP001")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "EMPLOYEE_IDENTIFIER_TYPE", "NATIONAL_ID"))
                .thenReturn(Optional.of(activeIdentifierTypeRuleEntity(RULE_SYSTEM_CODE, "NATIONAL_ID")));
        when(identifierRepository.findByEmployeeIdAndIdentifierTypeCode(10L, "NATIONAL_ID")).thenReturn(Optional.empty());

        assertThrows(IdentifierValueInvalidException.class, () -> service.create(command));
        verify(identifierRepository, never()).save(any(Identifier.class));
    }

    @Test
    void rejectsCreateWhenDuplicateIdentifierTypeExistsForEmployee() {
        CreateIdentifierCommand command = new CreateIdentifierCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                "EMP001",
                "NATIONAL_ID",
                "12345678A",
                null,
                null,
                false
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeIdentifierLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, "EMP001"))
                .thenReturn(Optional.of(employeeContext(10L, "EMP001")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "EMPLOYEE_IDENTIFIER_TYPE", "NATIONAL_ID"))
                .thenReturn(Optional.of(activeIdentifierTypeRuleEntity(RULE_SYSTEM_CODE, "NATIONAL_ID")));
        when(identifierRepository.findByEmployeeIdAndIdentifierTypeCode(10L, "NATIONAL_ID"))
                .thenReturn(Optional.of(existingIdentifier(10L, "NATIONAL_ID", false)));

        assertThrows(IdentifierAlreadyExistsException.class, () -> service.create(command));
        verify(identifierRepository, never()).save(any(Identifier.class));
    }

    @Test
    void rejectsCreateWhenPrimaryAlreadyExistsForEmployee() {
        CreateIdentifierCommand command = new CreateIdentifierCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                "EMP001",
                "PASSPORT",
                "PA123456",
                null,
                null,
                true
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeIdentifierLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, "EMP001"))
                .thenReturn(Optional.of(employeeContext(10L, "EMP001")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "EMPLOYEE_IDENTIFIER_TYPE", "PASSPORT"))
                .thenReturn(Optional.of(activeIdentifierTypeRuleEntity(RULE_SYSTEM_CODE, "PASSPORT")));
        when(identifierRepository.findByEmployeeIdAndIdentifierTypeCode(10L, "PASSPORT"))
                .thenReturn(Optional.empty());
        when(identifierRepository.existsByEmployeeIdAndIsPrimaryTrue(10L)).thenReturn(true);

        assertThrows(IdentifierPrimaryAlreadyExistsException.class, () -> service.create(command));
        verify(identifierRepository, never()).save(any(Identifier.class));
    }

    @Test
    void allowsNonPrimaryWhenPrimaryAlreadyExists() {
        CreateIdentifierCommand command = new CreateIdentifierCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                "EMP001",
                "PASSPORT",
                "PA123456",
                null,
                null,
                false
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeIdentifierLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, "EMP001"))
                .thenReturn(Optional.of(employeeContext(10L, "EMP001")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "EMPLOYEE_IDENTIFIER_TYPE", "PASSPORT"))
                .thenReturn(Optional.of(activeIdentifierTypeRuleEntity(RULE_SYSTEM_CODE, "PASSPORT")));
        when(identifierRepository.findByEmployeeIdAndIdentifierTypeCode(10L, "PASSPORT")).thenReturn(Optional.empty());
        when(identifierRepository.save(any(Identifier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Identifier created = service.create(command);

        assertEquals("PASSPORT", created.getIdentifierTypeCode());
        assertEquals(false, created.isPrimary());
    }

    @Test
    void rejectsCreateWhenSpanishNationalIdLetterIsInvalid() {
        CreateIdentifierCommand command = new CreateIdentifierCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                "EMP001",
                "NATIONAL_ID",
                "12345678A",
                "ESP",
                null,
                false
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeIdentifierLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, "EMP001"))
                .thenReturn(Optional.of(employeeContext(10L, "EMP001")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "EMPLOYEE_IDENTIFIER_TYPE", "NATIONAL_ID"))
                .thenReturn(Optional.of(activeIdentifierTypeRuleEntity(RULE_SYSTEM_CODE, "NATIONAL_ID")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "COUNTRY", "ESP"))
                .thenReturn(Optional.of(activeCountryRuleEntity(RULE_SYSTEM_CODE, "ESP")));
        IdentifierSpanishNationalIdInvalidException ex =
                assertThrows(IdentifierSpanishNationalIdInvalidException.class, () -> service.create(command));

        // El mensaje es el que lee quien lo tecleó, con el motivo (backend#171).
        assertEquals("El DNI 12345678A no es válido: la letra debería ser Z.", ex.getMessage());
        verify(identifierRepository, never()).save(any(Identifier.class));
    }

    @Test
    void rejectsCreateWhenSpanishNationalIdFormatIsInvalid() {
        CreateIdentifierCommand command = new CreateIdentifierCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                "EMP001",
                "NATIONAL_ID",
                "1234567L",
                "ESP",
                null,
                false
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeIdentifierLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, "EMP001"))
                .thenReturn(Optional.of(employeeContext(10L, "EMP001")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "EMPLOYEE_IDENTIFIER_TYPE", "NATIONAL_ID"))
                .thenReturn(Optional.of(activeIdentifierTypeRuleEntity(RULE_SYSTEM_CODE, "NATIONAL_ID")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "COUNTRY", "ESP"))
                .thenReturn(Optional.of(activeCountryRuleEntity(RULE_SYSTEM_CODE, "ESP")));
        assertThrows(IdentifierSpanishNationalIdInvalidException.class, () -> service.create(command));
        verify(identifierRepository, never()).save(any(Identifier.class));
    }

    @Test
    void normalizesCreateWhenSpanishNationalIdHasSpacesAndLowercase() {
        CreateIdentifierCommand command = new CreateIdentifierCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                "EMP001",
                "NATIONAL_ID",
                " 12345678z ",
                "ESP",
                null,
                false
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeIdentifierLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, "EMP001"))
                .thenReturn(Optional.of(employeeContext(10L, "EMP001")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "EMPLOYEE_IDENTIFIER_TYPE", "NATIONAL_ID"))
                .thenReturn(Optional.of(activeIdentifierTypeRuleEntity(RULE_SYSTEM_CODE, "NATIONAL_ID")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "COUNTRY", "ESP"))
                .thenReturn(Optional.of(activeCountryRuleEntity(RULE_SYSTEM_CODE, "ESP")));
        when(identifierRepository.findByEmployeeIdAndIdentifierTypeCode(10L, "NATIONAL_ID")).thenReturn(Optional.empty());
        when(identifierRepository.save(any(Identifier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Identifier created = service.create(command);

        assertEquals("12345678Z", created.getIdentifierValue());
    }

    @Test
    void doesNotApplySpanishNationalIdValidationWhenCountryIsNotSpain() {
        CreateIdentifierCommand command = new CreateIdentifierCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                "EMP001",
                "NATIONAL_ID",
                "12345678A",
                "PRT",
                null,
                false
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeIdentifierLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, "EMP001"))
                .thenReturn(Optional.of(employeeContext(10L, "EMP001")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "EMPLOYEE_IDENTIFIER_TYPE", "NATIONAL_ID"))
                .thenReturn(Optional.of(activeIdentifierTypeRuleEntity(RULE_SYSTEM_CODE, "NATIONAL_ID")));
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM_CODE, "COUNTRY", "PRT"))
                .thenReturn(Optional.of(activeCountryRuleEntity(RULE_SYSTEM_CODE, "PRT")));
        when(identifierRepository.findByEmployeeIdAndIdentifierTypeCode(10L, "NATIONAL_ID")).thenReturn(Optional.empty());
        when(identifierRepository.save(any(Identifier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Identifier created = service.create(command);

        assertEquals("12345678A", created.getIdentifierValue());
    }

    private EmployeeIdentifierContext employeeContext(Long employeeId, String employeeNumber) {
        return new EmployeeIdentifierContext(employeeId, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber);
    }

    private RuleSystem ruleSystem(String code) {
        return new RuleSystem(
                1L,
                code,
                "Spain",
                "ESP",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private RuleEntity activeIdentifierTypeRuleEntity(String ruleSystemCode, String code) {
        return new RuleEntity(
                1L,
                ruleSystemCode,
                "EMPLOYEE_IDENTIFIER_TYPE",
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

        private RuleEntity activeCountryRuleEntity(String ruleSystemCode, String code) {
                return new RuleEntity(
                                2L,
                                ruleSystemCode,
                                "COUNTRY",
                                code,
                                code,
                                "Country",
                                true,
                                LocalDate.of(1900, 1, 1),
                                null,
                                LocalDateTime.now(),
                                LocalDateTime.now()
                );
        }

    private Identifier existingIdentifier(Long employeeId, String identifierTypeCode, boolean isPrimary) {
        return new Identifier(
                20L,
                employeeId,
                identifierTypeCode,
                "EXISTING",
                null,
                null,
                isPrimary,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
