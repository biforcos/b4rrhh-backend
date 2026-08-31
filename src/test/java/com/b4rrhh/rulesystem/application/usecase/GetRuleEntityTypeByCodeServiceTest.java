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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRuleEntityTypeByCodeServiceTest {

    @Mock private RuleEntityTypeRepository ruleEntityTypeRepository;

    private GetRuleEntityTypeByCodeService service;

    @BeforeEach
    void setUp() {
        service = new GetRuleEntityTypeByCodeService(ruleEntityTypeRepository);
    }

    @Test
    void returnsTypeWhenFound() {
        RuleEntityType type = new RuleEntityType(1L, "COMPANY", "Company",
                LiteralClass.PROPER_NOUN, MaintenanceMode.MAINTAINED, "ORGANIZATION",
                true, null, null);
        when(ruleEntityTypeRepository.findByCode("COMPANY")).thenReturn(Optional.of(type));

        Optional<RuleEntityType> result = service.getByCode("COMPANY");

        assertTrue(result.isPresent());
        assertEquals("COMPANY", result.get().getCode());
    }

    @Test
    void returnsEmptyWhenNotFound() {
        when(ruleEntityTypeRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        Optional<RuleEntityType> result = service.getByCode("UNKNOWN");

        assertTrue(result.isEmpty());
    }

    @Test
    void normalizesCodeBeforeLookup() {
        when(ruleEntityTypeRepository.findByCode("COMPANY")).thenReturn(Optional.empty());

        service.getByCode(" company ");

        verify(ruleEntityTypeRepository).findByCode("COMPANY");
    }
}
