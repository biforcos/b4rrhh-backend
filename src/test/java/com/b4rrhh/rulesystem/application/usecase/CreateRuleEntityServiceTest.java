package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.application.port.RuleEntityTypeOwnEndpointPort;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityTypeIsMaintainedByItsOwnEndpointException;
import com.b4rrhh.rulesystem.domain.model.LiteralClass;
import com.b4rrhh.rulesystem.domain.model.MaintenanceMode;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.model.RuleEntityExtension;
import com.b4rrhh.rulesystem.domain.model.RuleEntityType;
import com.b4rrhh.rulesystem.domain.model.RuleSystem;
import com.b4rrhh.rulesystem.domain.port.RuleEntityExtensionRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityTypeRepository;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateRuleEntityServiceTest {

    @Mock private RuleEntityRepository ruleEntityRepository;
    @Mock private RuleSystemRepository ruleSystemRepository;
    @Mock private RuleEntityTypeRepository ruleEntityTypeRepository;
    @Mock private RuleEntityExtensionRepository ruleEntityExtensionRepository;
    @Mock private RuleEntityTypeOwnEndpointPort ruleEntityTypeOwnEndpointPort;

    private CreateRuleEntityService service;

    @BeforeEach
    void setUp() {
        service = new CreateRuleEntityService(
                ruleEntityRepository,
                ruleSystemRepository,
                ruleEntityTypeRepository,
                ruleEntityExtensionRepository,
                ruleEntityTypeOwnEndpointPort);
    }

    @Test
    void createsEntitySuccessfully() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem()));
        when(ruleEntityTypeRepository.findByCode("COST_CENTER")).thenReturn(Optional.of(ruleEntityType()));
        when(ruleEntityRepository.findByBusinessKey("ESP", "COST_CENTER", "CC01")).thenReturn(Optional.empty());
        when(ruleEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleEntity result = service.create(new CreateRuleEntityCommand(
                "ESP", "COST_CENTER", "CC01", "Centro de coste 01", "Description", LocalDate.of(2020, 1, 1), null));

        assertEquals("ESP", result.getRuleSystemCode());
        assertEquals("COST_CENTER", result.getRuleEntityTypeCode());
        assertEquals("CC01", result.getCode());
        assertEquals("Centro de coste 01", result.getName());
        assertTrue(result.isActive());
        verify(ruleEntityRepository).save(any());
    }

    @Test
    void normalizesCodesAndStripsWhitespace() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem()));
        when(ruleEntityTypeRepository.findByCode("COST_CENTER")).thenReturn(Optional.of(ruleEntityType()));
        when(ruleEntityRepository.findByBusinessKey("ESP", "COST_CENTER", "CC01")).thenReturn(Optional.empty());
        when(ruleEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleEntity result = service.create(new CreateRuleEntityCommand(
                " esp ", " cost_center ", " cc01 ", "  Centro de coste 01  ", null, LocalDate.of(2020, 1, 1), null));

        assertEquals("ESP", result.getRuleSystemCode());
        assertEquals("COST_CENTER", result.getRuleEntityTypeCode());
        assertEquals("CC01", result.getCode());
        assertEquals("Centro de coste 01", result.getName());
    }

    @Test
    void normalizesEmptyDescriptionToNull() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem()));
        when(ruleEntityTypeRepository.findByCode("COST_CENTER")).thenReturn(Optional.of(ruleEntityType()));
        when(ruleEntityRepository.findByBusinessKey("ESP", "COST_CENTER", "CC01")).thenReturn(Optional.empty());
        when(ruleEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleEntity result = service.create(new CreateRuleEntityCommand(
                "ESP", "COST_CENTER", "CC01", "Name", "   ", LocalDate.of(2020, 1, 1), null));

        assertNull(result.getDescription());
    }

    @Test
    void failsWhenRuleSystemNotFound() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                service.create(new CreateRuleEntityCommand("ESP", "COST_CENTER", "CC01", "Name", null, LocalDate.of(2020, 1, 1), null)));

        verify(ruleEntityRepository, never()).save(any());
    }

    @Test
    void failsWhenRuleEntityTypeNotFound() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem()));
        when(ruleEntityTypeRepository.findByCode("COST_CENTER")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                service.create(new CreateRuleEntityCommand("ESP", "COST_CENTER", "CC01", "Name", null, LocalDate.of(2020, 1, 1), null)));

        verify(ruleEntityRepository, never()).save(any());
    }

    @Test
    void failsWhenEntityWithSameBusinessKeyAlreadyExists() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem()));
        when(ruleEntityTypeRepository.findByCode("COST_CENTER")).thenReturn(Optional.of(ruleEntityType()));
        RuleEntity existing = new RuleEntity(1L, "ESP", "COST_CENTER", "CC01", "Existing", null, true,
                LocalDate.of(2020, 1, 1), null, null, null);
        when(ruleEntityRepository.findByBusinessKey("ESP", "COST_CENTER", "CC01"))
                .thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () ->
                service.create(new CreateRuleEntityCommand("ESP", "COST_CENTER", "CC01", "Name", null, LocalDate.of(2020, 1, 1), null)));

        verify(ruleEntityRepository, never()).save(any());
    }

    // backend#88. La pregunta se le hace al metamodelo: el tipo se rechaza porque declara una
    // extension required, no porque su codigo este escrito en ninguna lista. Por eso estos dos
    // tests usan un tipo inventado —COMPANY no aparece— y aun asi salen rechazados.
    @Test
    void rejectsATypeThatDeclaresRequiredExtensionsAndSaysWhereToGoInstead() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem()));
        when(ruleEntityTypeRepository.findByCode("ZZ_WITH_PROFILE")).thenReturn(Optional.of(ruleEntityType()));
        when(ruleEntityExtensionRepository.findRequiredByRuleEntityTypeCode("ZZ_WITH_PROFILE"))
                .thenReturn(List.of(new RuleEntityExtension(
                        "ZZ_WITH_PROFILE", "PROFILE", "rulesystem.zz_profile", "1:1", true)));
        when(ruleEntityTypeOwnEndpointPort.findApiCollectionPath("ZZ_WITH_PROFILE"))
                .thenReturn(Optional.of("/zz-things"));

        RuleEntityTypeIsMaintainedByItsOwnEndpointException thrown = assertThrows(
                RuleEntityTypeIsMaintainedByItsOwnEndpointException.class,
                () -> service.create(new CreateRuleEntityCommand(
                        "ESP", "ZZ_WITH_PROFILE", "X1", "Name", null, LocalDate.of(2020, 1, 1), null)));

        assertTrue(thrown.getMessage().contains("POST /zz-things"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("rulesystem.zz_profile"), thrown.getMessage());
        verify(ruleEntityRepository, never()).save(any());
    }

    // Un tipo con extension obligatoria y sin endpoint propio todavia —AGREEMENT hoy— no puede
    // inventarse una ruta plausible: el mensaje dice que no hay ninguna, que es lo que sabe.
    @Test
    void saysThereIsNoEndpointYetWhenTheTypeDeclaresNone() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem()));
        when(ruleEntityTypeRepository.findByCode("ZZ_WITH_PROFILE")).thenReturn(Optional.of(ruleEntityType()));
        when(ruleEntityExtensionRepository.findRequiredByRuleEntityTypeCode("ZZ_WITH_PROFILE"))
                .thenReturn(List.of(new RuleEntityExtension(
                        "ZZ_WITH_PROFILE", "PROFILE", "rulesystem.zz_profile", "1:1", true)));
        when(ruleEntityTypeOwnEndpointPort.findApiCollectionPath("ZZ_WITH_PROFILE"))
                .thenReturn(Optional.empty());

        RuleEntityTypeIsMaintainedByItsOwnEndpointException thrown = assertThrows(
                RuleEntityTypeIsMaintainedByItsOwnEndpointException.class,
                () -> service.create(new CreateRuleEntityCommand(
                        "ESP", "ZZ_WITH_PROFILE", "X1", "Name", null, LocalDate.of(2020, 1, 1), null)));

        assertTrue(thrown.getMessage().contains("no creation endpoint of its own yet"), thrown.getMessage());
        assertFalse(thrown.getMessage().contains("Use POST"), thrown.getMessage());
        verify(ruleEntityRepository, never()).save(any());
    }

    private RuleSystem ruleSystem() {
        return new RuleSystem(1L, "ESP", "Spain", "ES", true, null, null);
    }

    private RuleEntityType ruleEntityType() {
        return new RuleEntityType(1L, "COST_CENTER", "Cost center",
                LiteralClass.DOMAIN_VOCABULARY, MaintenanceMode.MAINTAINED, "ORGANIZATION",
                true, null, null);
    }
}
