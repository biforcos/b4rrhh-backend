package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.model.LiteralClass;
import com.b4rrhh.rulesystem.domain.model.MaintenanceMode;
import com.b4rrhh.rulesystem.domain.model.RuleEntityType;
import com.b4rrhh.rulesystem.domain.port.RuleEntityTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateRuleEntityTypeServiceTest {

    @Mock private RuleEntityTypeRepository ruleEntityTypeRepository;

    private CreateRuleEntityTypeService service;

    @BeforeEach
    void setUp() {
        service = new CreateRuleEntityTypeService(ruleEntityTypeRepository);
    }

    @Test
    void createsTypeSuccessfully() {
        when(ruleEntityTypeRepository.findByCode("COMPANY")).thenReturn(Optional.empty());
        when(ruleEntityTypeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleEntityType result = service.create(new CreateRuleEntityTypeCommand(
                "COMPANY", "Company", LiteralClass.PROPER_NOUN, MaintenanceMode.MAINTAINED, "ORGANIZATION"));

        assertEquals("COMPANY", result.getCode());
        assertEquals("Company", result.getName());
        assertEquals(LiteralClass.PROPER_NOUN, result.getLiteralClass());
        assertEquals(MaintenanceMode.MAINTAINED, result.getMaintenanceMode());
        assertEquals("ORGANIZATION", result.getGroupCode());
        assertTrue(result.isActive());
        verify(ruleEntityTypeRepository).save(any());
    }

    @Test
    void normalizesCodeToUpperCase() {
        when(ruleEntityTypeRepository.findByCode("COMPANY")).thenReturn(Optional.empty());
        when(ruleEntityTypeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleEntityType result = service.create(new CreateRuleEntityTypeCommand(
                " company ", "  Company  ", LiteralClass.PROPER_NOUN, MaintenanceMode.MAINTAINED, " organization "));

        assertEquals("COMPANY", result.getCode());
        assertEquals("Company", result.getName());
        assertEquals("ORGANIZATION", result.getGroupCode());
    }

    @Test
    void failsWhenTypeWithSameCodeAlreadyExists() {
        when(ruleEntityTypeRepository.findByCode("COMPANY"))
                .thenReturn(Optional.of(new RuleEntityType(
                        1L, "COMPANY", "Company",
                        LiteralClass.PROPER_NOUN, MaintenanceMode.MAINTAINED, "ORGANIZATION",
                        true, null, null)));

        assertThrows(IllegalArgumentException.class, () ->
                service.create(new CreateRuleEntityTypeCommand(
                        "COMPANY", "Company",
                        LiteralClass.PROPER_NOUN, MaintenanceMode.MAINTAINED, "ORGANIZATION")));

        verify(ruleEntityTypeRepository, never()).save(any());
    }

    // ADR-054 §6: añadir un tipo cuesta tres decisiones; sin una de ellas no se guarda nada.
    @Test
    void failsWhenAClassificationDecisionIsMissing() {
        assertThrows(IllegalArgumentException.class, () ->
                service.create(new CreateRuleEntityTypeCommand(
                        "COMPANY", "Company",
                        null, MaintenanceMode.MAINTAINED, "ORGANIZATION")));

        verify(ruleEntityTypeRepository, never()).save(any());
    }
}
