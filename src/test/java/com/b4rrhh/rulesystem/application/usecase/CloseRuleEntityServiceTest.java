package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.exception.RuleEntityAlreadyClosedException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityInvalidDateRangeException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityNotFoundException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityOverlapException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class CloseRuleEntityServiceTest {

    @Mock
    private RuleEntityRepository ruleEntityRepository;

    private CloseRuleEntityService service;

    @BeforeEach
    void setUp() {
        service = new CloseRuleEntityService(ruleEntityRepository, mock(NationalLayerGuard.class));
    }

    @Test
    void closesOpenOccurrenceSuccessfully() {
        LocalDate startDate = LocalDate.of(1900, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        RuleEntity existing = ruleEntity(startDate, null);

        when(ruleEntityRepository.findByBusinessKeyAndStartDate("ESP", "COMPANY", "ES01", startDate))
                .thenReturn(Optional.of(existing));
        when(ruleEntityRepository.existsOverlapExcludingStartDate("ESP", "COMPANY", "ES01", startDate, endDate, startDate))
                .thenReturn(false);
        when(ruleEntityRepository.save(existing)).thenReturn(existing);

        RuleEntity result = service.close(new CloseRuleEntityCommand("ESP", "COMPANY", "ES01", startDate, endDate));

        assertEquals(endDate, result.getEndDate());
        assertEquals(false, result.isActive());
        verify(ruleEntityRepository).existsOverlapExcludingStartDate(
                "ESP",
                "COMPANY",
                "ES01",
                startDate,
                endDate,
                startDate
        );
        verify(ruleEntityRepository).save(existing);
    }

    @Test
    void failsWhenOccurrenceDoesNotExist() {
        LocalDate startDate = LocalDate.of(1900, 1, 1);
        when(ruleEntityRepository.findByBusinessKeyAndStartDate("ESP", "COMPANY", "ES01", startDate))
                .thenReturn(Optional.empty());

        assertThrows(
                RuleEntityNotFoundException.class,
                () -> service.close(new CloseRuleEntityCommand("ESP", "COMPANY", "ES01", startDate, LocalDate.of(2026, 12, 31)))
        );
    }

    @Test
    void failsWhenOccurrenceIsAlreadyClosed() {
        LocalDate startDate = LocalDate.of(1900, 1, 1);
        RuleEntity existing = ruleEntity(startDate, LocalDate.of(2025, 12, 31));
        when(ruleEntityRepository.findByBusinessKeyAndStartDate("ESP", "COMPANY", "ES01", startDate))
                .thenReturn(Optional.of(existing));

        assertThrows(
                RuleEntityAlreadyClosedException.class,
                () -> service.close(new CloseRuleEntityCommand("ESP", "COMPANY", "ES01", startDate, LocalDate.of(2026, 12, 31)))
        );

        verify(ruleEntityRepository, never()).save(existing);
    }

    @Test
    void failsWhenEndDateIsBeforeStartDate() {
        LocalDate startDate = LocalDate.of(1900, 1, 1);
        RuleEntity existing = ruleEntity(startDate, null);
        when(ruleEntityRepository.findByBusinessKeyAndStartDate("ESP", "COMPANY", "ES01", startDate))
                .thenReturn(Optional.of(existing));

        assertThrows(
                RuleEntityInvalidDateRangeException.class,
                () -> service.close(new CloseRuleEntityCommand("ESP", "COMPANY", "ES01", startDate, LocalDate.of(1899, 12, 31)))
        );

        verify(ruleEntityRepository, never()).save(existing);
    }

    @Test
    void failsWhenClosingCausesOverlap() {
        LocalDate startDate = LocalDate.of(1900, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        RuleEntity existing = ruleEntity(startDate, null);
        when(ruleEntityRepository.findByBusinessKeyAndStartDate("ESP", "COMPANY", "ES01", startDate))
                .thenReturn(Optional.of(existing));
        when(ruleEntityRepository.existsOverlapExcludingStartDate("ESP", "COMPANY", "ES01", startDate, endDate, startDate))
                .thenReturn(true);

        assertThrows(
                RuleEntityOverlapException.class,
                () -> service.close(new CloseRuleEntityCommand("ESP", "COMPANY", "ES01", startDate, endDate))
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
