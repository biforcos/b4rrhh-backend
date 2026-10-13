package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.exception.RuleEntityInvalidDateRangeException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityNotFoundException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityOverlapException;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrectRuleEntityServiceTest {

    @Mock
    private RuleEntityRepository ruleEntityRepository;

    private CorrectRuleEntityService service;

    @BeforeEach
    void setUp() {
        service = new CorrectRuleEntityService(ruleEntityRepository, mock(NationalLayerGuard.class));
    }

    @Test
    void correctsMutableFieldsAndPreservesIdentity() {
        LocalDate startDate = LocalDate.of(1900, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        RuleEntity existing = ruleEntity(startDate, null);

        when(ruleEntityRepository.findByBusinessKeyAndStartDate("ESP", "COMPANY", "ES01", startDate))
                .thenReturn(Optional.of(existing));
        when(ruleEntityRepository.existsOverlapExcludingStartDate("ESP", "COMPANY", "ES01", startDate, endDate, startDate))
                .thenReturn(false);
        when(ruleEntityRepository.save(existing)).thenReturn(existing);

        RuleEntity result = service.correct(new CorrectRuleEntityCommand(
                "esp", "company", "es01", startDate,
                "Company corrected", "Corrected description", endDate
        ));

        ArgumentCaptor<RuleEntity> captor = ArgumentCaptor.forClass(RuleEntity.class);
        verify(ruleEntityRepository).save(captor.capture());
        RuleEntity persisted = captor.getValue();

        assertEquals("ESP", persisted.getRuleSystemCode());
        assertEquals("COMPANY", persisted.getRuleEntityTypeCode());
        assertEquals("ES01", persisted.getCode());
        assertEquals(startDate, persisted.getStartDate());
        assertEquals("Company corrected", persisted.getName());
        assertEquals("Corrected description", persisted.getDescription());
        assertEquals(endDate, persisted.getEndDate());
        assertEquals(false, persisted.isActive());
        assertEquals("ES01", result.getCode());
    }

    @Test
    void throwsNotFoundWhenOccurrenceDoesNotExist() {
        LocalDate startDate = LocalDate.of(1900, 1, 1);
        when(ruleEntityRepository.findByBusinessKeyAndStartDate("ESP", "COMPANY", "ES01", startDate))
                .thenReturn(Optional.empty());

        assertThrows(
                RuleEntityNotFoundException.class,
                () -> service.correct(new CorrectRuleEntityCommand(
                        "ESP", "COMPANY", "ES01", startDate,
                        "Company corrected", null, null
                ))
        );
    }

    @Test
    void throwsConflictWhenEndDateIsBeforeStartDate() {
        LocalDate startDate = LocalDate.of(1900, 1, 1);
        RuleEntity existing = ruleEntity(startDate, null);
        when(ruleEntityRepository.findByBusinessKeyAndStartDate("ESP", "COMPANY", "ES01", startDate))
                .thenReturn(Optional.of(existing));

        assertThrows(
                RuleEntityInvalidDateRangeException.class,
                () -> service.correct(new CorrectRuleEntityCommand(
                        "ESP", "COMPANY", "ES01", startDate,
                        "Company corrected", null, LocalDate.of(1899, 12, 31)
                ))
        );

        verify(ruleEntityRepository, never()).save(existing);
    }

    @Test
    void throwsConflictWhenCorrectionCausesOverlap() {
        LocalDate startDate = LocalDate.of(1900, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        RuleEntity existing = ruleEntity(startDate, null);

        when(ruleEntityRepository.findByBusinessKeyAndStartDate("ESP", "COMPANY", "ES01", startDate))
                .thenReturn(Optional.of(existing));
        when(ruleEntityRepository.existsOverlapExcludingStartDate("ESP", "COMPANY", "ES01", startDate, endDate, startDate))
                .thenReturn(true);

        assertThrows(
                RuleEntityOverlapException.class,
                () -> service.correct(new CorrectRuleEntityCommand(
                        "ESP", "COMPANY", "ES01", startDate,
                        "Company corrected", null, endDate
                ))
        );

        verify(ruleEntityRepository, never()).save(existing);
    }

    private RuleEntity ruleEntity(LocalDate startDate, LocalDate endDate) {
        return new RuleEntity(
                1L,
                "ESP",
                "COMPANY",
                "ES01",
                "Company",
                null,
                endDate == null,
                startDate,
                endDate,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
