package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.application.port.RuleSystemLayerPort;
import com.b4rrhh.rulesystem.domain.model.RuleSystem;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
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
class CreateRuleSystemServiceTest {

    @Mock private RuleSystemRepository ruleSystemRepository;
    @Mock private RuleSystemLayerPort ruleSystemLayerPort;

    private CreateRuleSystemService service;

    @BeforeEach
    void setUp() {
        service = new CreateRuleSystemService(ruleSystemRepository, ruleSystemLayerPort);
    }

    @Test
    void createsRuleSystemSuccessfully() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.empty());
        when(ruleSystemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleSystem result = service.create(new CreateRuleSystemCommand("ESP", "Spain", "ES"));

        assertEquals("ESP", result.getCode());
        assertEquals("Spain", result.getName());
        assertEquals("ES", result.getCountryCode());
        assertTrue(result.isActive());
        verify(ruleSystemRepository).save(any());
    }

    @Test
    void normalizesCodeAndCountryCodeToUpperCase() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.empty());
        when(ruleSystemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleSystem result = service.create(new CreateRuleSystemCommand(" esp ", "  Spain  ", " es "));

        assertEquals("ESP", result.getCode());
        assertEquals("Spain", result.getName());
        assertEquals("ES", result.getCountryCode());
    }

    @Test
    void failsWhenRuleSystemWithSameCodeAlreadyExists() {
        when(ruleSystemRepository.findByCode("ESP"))
                .thenReturn(Optional.of(new RuleSystem(1L, "ESP", "Spain", "ES", true, null, null)));

        assertThrows(IllegalArgumentException.class, () ->
                service.create(new CreateRuleSystemCommand("ESP", "Spain", "ES")));

        verify(ruleSystemRepository, never()).save(any());
    }

    // backend#156: una reglamentacion es un puzle de cinco capas (ADR-077). La que se crea por la
    // API trae las suyas: la nacional con su mismo codigo, NOM_<codigo> y NOM_<codigo>_EMP, y
    // monta las comunes COM e INT.
    @Test
    void assemblesItsFiveLayersWhenCreated() {
        when(ruleSystemRepository.findByCode("AND")).thenReturn(Optional.empty());
        when(ruleSystemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(new CreateRuleSystemCommand("and", "Andorra", "AND"));

        verify(ruleSystemLayerPort).assembleDefaultLayers("AND", "Andorra");
    }

    @Test
    void failsWhenOneOfItsLayerCodesIsAlreadyTaken() {
        when(ruleSystemRepository.findByCode("INT")).thenReturn(Optional.empty());
        when(ruleSystemLayerPort.layerExists("INT")).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                service.create(new CreateRuleSystemCommand("INT", "Interior", "ESP")));

        assertTrue(error.getMessage().contains("INT"));
        verify(ruleSystemRepository, never()).save(any());
        verify(ruleSystemLayerPort, never()).assembleDefaultLayers(any(), any());
    }
}
