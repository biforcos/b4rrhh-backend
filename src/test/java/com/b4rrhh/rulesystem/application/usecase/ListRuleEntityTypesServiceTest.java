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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListRuleEntityTypesServiceTest {

    @Mock private RuleEntityTypeRepository ruleEntityTypeRepository;

    private ListRuleEntityTypesService service;

    @BeforeEach
    void setUp() {
        service = new ListRuleEntityTypesService(ruleEntityTypeRepository);
    }

    @Test
    void returnsAllRuleEntityTypes() {
        List<RuleEntityType> types = List.of(
                new RuleEntityType(1L, "COMPANY", "Company",
                        LiteralClass.PROPER_NOUN, MaintenanceMode.MAINTAINED, "ORGANIZATION",
                        true, null, null),
                new RuleEntityType(2L, "WORK_CENTER", "Work Center",
                        LiteralClass.PROPER_NOUN, MaintenanceMode.MAINTAINED, "ORGANIZATION",
                        true, null, null)
        );
        when(ruleEntityTypeRepository.findAll()).thenReturn(types);

        List<RuleEntityType> result = service.listAll();

        assertEquals(2, result.size());
        assertEquals("COMPANY", result.get(0).getCode());
        assertEquals("WORK_CENTER", result.get(1).getCode());
    }

    @Test
    void returnsEmptyListWhenNoTypes() {
        when(ruleEntityTypeRepository.findAll()).thenReturn(List.of());

        List<RuleEntityType> result = service.listAll();

        assertEquals(0, result.size());
    }
}
