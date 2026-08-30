package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.application.port.RuleEntityUsageCheckPort;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityInUseException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityNotFoundException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.model.RuleEntityReference;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteRuleEntityServiceTest {

    @Mock
    private RuleEntityRepository ruleEntityRepository;
    @Mock
    private RuleEntityUsageCheckPort ruleEntityUsageCheckPort;

    private DeleteRuleEntityService service;

    @BeforeEach
    void setUp() {
        service = new DeleteRuleEntityService(ruleEntityRepository, ruleEntityUsageCheckPort);
    }

    @Test
    void deletesRuleEntityWhenExistsAndIsNotUsed() {
        LocalDate startDate = LocalDate.of(1900, 1, 1);
        when(ruleEntityRepository.findByBusinessKeyAndStartDate("ESP", "COMPANY", "ES01", startDate))
                .thenReturn(Optional.of(ruleEntity(startDate)));
        when(ruleEntityUsageCheckPort.findReferences("ESP", "COMPANY", "ES01"))
                .thenReturn(List.of());

        service.delete(new DeleteRuleEntityCommand("esp", "company", "es01", startDate));

        verify(ruleEntityRepository).deleteByBusinessKeyAndStartDate("ESP", "COMPANY", "ES01", startDate);
    }

    @Test
    void throwsNotFoundWhenRuleEntityDoesNotExist() {
        LocalDate startDate = LocalDate.of(1900, 1, 1);
        when(ruleEntityRepository.findByBusinessKeyAndStartDate("ESP", "COMPANY", "ES01", startDate))
                .thenReturn(Optional.empty());

        assertThrows(
                RuleEntityNotFoundException.class,
                () -> service.delete(new DeleteRuleEntityCommand("ESP", "COMPANY", "ES01", startDate))
        );

        verify(ruleEntityUsageCheckPort, never()).findReferences("ESP", "COMPANY", "ES01");
        verify(ruleEntityRepository, never()).deleteByBusinessKeyAndStartDate("ESP", "COMPANY", "ES01", startDate);
    }

    @Test
    void throwsConflictWhenRuleEntityIsUsed() {
        LocalDate startDate = LocalDate.of(1900, 1, 1);
        when(ruleEntityRepository.findByBusinessKeyAndStartDate("ESP", "COMPANY", "ES01", startDate))
                .thenReturn(Optional.of(ruleEntity(startDate)));
        when(ruleEntityUsageCheckPort.findReferences("ESP", "COMPANY", "ES01"))
                .thenReturn(List.of(new RuleEntityReference("presences", 412), new RuleEntityReference("work-centers", 3)));

        RuleEntityInUseException exception = assertThrows(
                RuleEntityInUseException.class,
                () -> service.delete(new DeleteRuleEntityCommand("ESP", "COMPANY", "ES01", startDate))
        );

        assertEquals(
                "Rule entity is in use: ESP/COMPANY/ES01 is referenced by 412 presences, 3 work-centers",
                exception.getMessage()
        );

        verify(ruleEntityRepository, never()).deleteByBusinessKeyAndStartDate("ESP", "COMPANY", "ES01", startDate);
    }

    private RuleEntity ruleEntity(LocalDate startDate) {
        return new RuleEntity(
                1L,
                "ESP",
                "COMPANY",
                "ES01",
                "Company",
                null,
                true,
                startDate,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
