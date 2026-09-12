package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollConcept;
import com.b4rrhh.payroll.domain.model.PayrollContextSnapshot;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollPersistenceAdapterTest {

    @Mock
    private SpringDataPayrollRepository springDataPayrollRepository;

    private PayrollPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PayrollPersistenceAdapter(springDataPayrollRepository);
    }

    @Test
    void saveExistingPayrollUpdatesHeaderWithoutRecreatingConceptsOrSnapshots() {
        PayrollEntity existingEntity = payrollEntity(PayrollStatus.CALCULATED, null);
        PayrollConceptEntity conceptEntity = conceptEntity();
        PayrollContextSnapshotEntity snapshotEntity = snapshotEntity();
        existingEntity.replaceConcepts(List.of(conceptEntity));
        existingEntity.replaceContextSnapshots(List.of(snapshotEntity));

        Payroll invalidated = Payroll.rehydrate(
                7L,
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                1,
                PayrollStatus.NOT_VALID,
                "USER_INVALIDATED",
                LocalDateTime.of(2026, 1, 31, 10, 15),
                "ENGINE",
                "1.0",
                List.of(new PayrollConcept(1, "BASE", "Base salary", new BigDecimal("1000.00"), null, null, "EARNING", "202501", 1)),
                List.of(new PayrollContextSnapshot("PRESENCE", "EMPLOYEE", "{\"presenceNumber\":1}", "{\"companyCode\":\"ES01\"}")),
                LocalDateTime.of(2026, 1, 31, 10, 15),
                LocalDateTime.of(2026, 1, 31, 10, 15)
        );

        when(springDataPayrollRepository.findById(7L)).thenReturn(Optional.of(existingEntity));
        when(springDataPayrollRepository.save(existingEntity)).thenReturn(existingEntity);

        adapter.save(invalidated);

        ArgumentCaptor<PayrollEntity> captor = ArgumentCaptor.forClass(PayrollEntity.class);
        verify(springDataPayrollRepository).save(captor.capture());
        PayrollEntity savedEntity = captor.getValue();
        assertSame(existingEntity, savedEntity);
        assertEquals(PayrollStatus.NOT_VALID, savedEntity.getStatus());
        assertEquals("USER_INVALIDATED", savedEntity.getStatusReasonCode());
        assertEquals(1, savedEntity.getConcepts().size());
        assertEquals(1, savedEntity.getContextSnapshots().size());
        assertSame(conceptEntity, savedEntity.getConcepts().iterator().next());
        assertSame(snapshotEntity, savedEntity.getContextSnapshots().iterator().next());
        assertEquals(41L, savedEntity.getRunId());
    }

    @Test
    void saveNewPayrollWritesTheRunThatProducedIt() {
        Payroll calculated = Payroll.create(
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                1,
                PayrollStatus.CALCULATED,
                null,
                LocalDateTime.of(2026, 1, 31, 10, 15),
                "ENGINE",
                "1.0",
                41L,
                List.of(),
                List.of(new PayrollConcept(1, "BASE", "Base salary", new BigDecimal("1000.00"), null, null, "EARNING", "202501", 1)),
                List.of(new PayrollContextSnapshot("PRESENCE", "EMPLOYEE", "{\"presenceNumber\":1}", "{\"companyCode\":\"ES01\"}")),
                List.of()
        );

        when(springDataPayrollRepository.save(any(PayrollEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adapter.save(calculated);

        ArgumentCaptor<PayrollEntity> captor = ArgumentCaptor.forClass(PayrollEntity.class);
        verify(springDataPayrollRepository).save(captor.capture());
        assertEquals(41L, captor.getValue().getRunId());
    }

    private PayrollEntity payrollEntity(PayrollStatus status, String statusReasonCode) {
        PayrollEntity payroll = new PayrollEntity();
        payroll.setId(7L);
        payroll.setRuleSystemCode("ESP");
        payroll.setEmployeeTypeCode("INTERNAL");
        payroll.setEmployeeNumber("EMP001");
        payroll.setPayrollPeriodCode("202501");
        payroll.setPayrollTypeCode("NORMAL");
        payroll.setPresenceNumber(1);
        payroll.setStatus(status);
        payroll.setStatusReasonCode(statusReasonCode);
        payroll.setCalculatedAt(LocalDateTime.of(2026, 1, 31, 10, 15));
        payroll.setCalculationEngineCode("ENGINE");
        payroll.setCalculationEngineVersion("1.0");
        payroll.setRunId(41L);
        payroll.setCreatedAt(LocalDateTime.of(2026, 1, 31, 10, 15));
        payroll.setUpdatedAt(LocalDateTime.of(2026, 1, 31, 10, 15));
        return payroll;
    }

    private PayrollConceptEntity conceptEntity() {
        PayrollConceptEntity concept = new PayrollConceptEntity();
        concept.setLineNumber(1);
        concept.setConceptCode("BASE");
        concept.setConceptLabel("Base salary");
        concept.setAmount(new BigDecimal("1000.00"));
        concept.setConceptNatureCode("EARNING");
        concept.setOriginPeriodCode("202501");
        concept.setDisplayOrder(1);
        return concept;
    }

    private PayrollContextSnapshotEntity snapshotEntity() {
        PayrollContextSnapshotEntity snapshot = new PayrollContextSnapshotEntity();
        snapshot.setSnapshotTypeCode("PRESENCE");
        snapshot.setSourceVerticalCode("EMPLOYEE");
        snapshot.setSourceBusinessKeyJson("{\"presenceNumber\":1}");
        snapshot.setSnapshotPayloadJson("{\"companyCode\":\"ES01\"}");
        return snapshot;
    }
}
