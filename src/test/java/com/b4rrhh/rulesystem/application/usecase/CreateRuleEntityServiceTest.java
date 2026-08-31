package com.b4rrhh.rulesystem.application.usecase;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateRuleEntityServiceTest {

    @Mock private RuleEntityRepository ruleEntityRepository;
    @Mock private RuleSystemRepository ruleSystemRepository;
    @Mock private RuleEntityTypeRepository ruleEntityTypeRepository;

    private CreateRuleEntityService service;

    @BeforeEach
    void setUp() {
        service = new CreateRuleEntityService(ruleEntityRepository, ruleSystemRepository, ruleEntityTypeRepository);
    }

    @Test
    void createsEntitySuccessfully() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem()));
        when(ruleEntityTypeRepository.findByCode("COMPANY")).thenReturn(Optional.of(ruleEntityType()));
        when(ruleEntityRepository.findByBusinessKey("ESP", "COMPANY", "ES01")).thenReturn(Optional.empty());
        when(ruleEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleEntity result = service.create(new CreateRuleEntityCommand(
                "ESP", "COMPANY", "ES01", "Company Spain", "Description", LocalDate.of(2020, 1, 1), null));

        assertEquals("ESP", result.getRuleSystemCode());
        assertEquals("COMPANY", result.getRuleEntityTypeCode());
        assertEquals("ES01", result.getCode());
        assertEquals("Company Spain", result.getName());
        assertTrue(result.isActive());
        verify(ruleEntityRepository).save(any());
    }

    @Test
    void normalizesCodesAndStripsWhitespace() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem()));
        when(ruleEntityTypeRepository.findByCode("COMPANY")).thenReturn(Optional.of(ruleEntityType()));
        when(ruleEntityRepository.findByBusinessKey("ESP", "COMPANY", "ES01")).thenReturn(Optional.empty());
        when(ruleEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleEntity result = service.create(new CreateRuleEntityCommand(
                " esp ", " company ", " es01 ", "  Company Spain  ", null, LocalDate.of(2020, 1, 1), null));

        assertEquals("ESP", result.getRuleSystemCode());
        assertEquals("COMPANY", result.getRuleEntityTypeCode());
        assertEquals("ES01", result.getCode());
        assertEquals("Company Spain", result.getName());
    }

    @Test
    void normalizesEmptyDescriptionToNull() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem()));
        when(ruleEntityTypeRepository.findByCode("COMPANY")).thenReturn(Optional.of(ruleEntityType()));
        when(ruleEntityRepository.findByBusinessKey("ESP", "COMPANY", "ES01")).thenReturn(Optional.empty());
        when(ruleEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleEntity result = service.create(new CreateRuleEntityCommand(
                "ESP", "COMPANY", "ES01", "Name", "   ", LocalDate.of(2020, 1, 1), null));

        assertNull(result.getDescription());
    }

    @Test
    void failsWhenRuleSystemNotFound() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                service.create(new CreateRuleEntityCommand("ESP", "COMPANY", "ES01", "Name", null, LocalDate.of(2020, 1, 1), null)));

        verify(ruleEntityRepository, never()).save(any());
    }

    @Test
    void failsWhenRuleEntityTypeNotFound() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem()));
        when(ruleEntityTypeRepository.findByCode("COMPANY")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                service.create(new CreateRuleEntityCommand("ESP", "COMPANY", "ES01", "Name", null, LocalDate.of(2020, 1, 1), null)));

        verify(ruleEntityRepository, never()).save(any());
    }

    @Test
    void failsWhenEntityWithSameBusinessKeyAlreadyExists() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem()));
        when(ruleEntityTypeRepository.findByCode("COMPANY")).thenReturn(Optional.of(ruleEntityType()));
        RuleEntity existing = new RuleEntity(1L, "ESP", "COMPANY", "ES01", "Existing", null, true,
                LocalDate.of(2020, 1, 1), null, null, null);
        when(ruleEntityRepository.findByBusinessKey("ESP", "COMPANY", "ES01"))
                .thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () ->
                service.create(new CreateRuleEntityCommand("ESP", "COMPANY", "ES01", "Name", null, LocalDate.of(2020, 1, 1), null)));

        verify(ruleEntityRepository, never()).save(any());
    }

    private RuleSystem ruleSystem() {
        return new RuleSystem(1L, "ESP", "Spain", "ES", true, null, null);
    }

    private RuleEntityType ruleEntityType() {
        return new RuleEntityType(1L, "COMPANY", "Company",
                LiteralClass.PROPER_NOUN, MaintenanceMode.MAINTAINED, "ORGANIZATION",
                true, null, null);
    }
}
