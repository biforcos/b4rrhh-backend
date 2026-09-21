package com.b4rrhh.payroll.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayrollEntityTest {

    @Test
    void replaceConceptsKeepsSetSemanticsByLineNumberWithinAggregate() {
        PayrollEntity payroll = payrollEntity();

        PayrollConceptEntity first = new PayrollConceptEntity();
        first.setLineNumber(1);
        first.setConceptCode("BASE");
        first.setConceptLabel("Base salary");
        first.setAmount(new BigDecimal("1000.00"));
        first.setConceptNatureCode("EARNING");
        first.setDisplayOrder(1);

        PayrollConceptEntity duplicateLine = new PayrollConceptEntity();
        duplicateLine.setLineNumber(1);
        duplicateLine.setConceptCode("BONUS");
        duplicateLine.setConceptLabel("Bonus");
        duplicateLine.setAmount(new BigDecimal("100.00"));
        duplicateLine.setConceptNatureCode("EARNING");
        duplicateLine.setDisplayOrder(2);

        payroll.replaceConcepts(List.of(first, duplicateLine));

        assertEquals(1, payroll.getConcepts().size());
    }

    @Test
    void replaceContextSnapshotsKeepsSetSemanticsBySnapshotTypeWithinAggregate() {
        PayrollEntity payroll = payrollEntity();

        PayrollContextSnapshotEntity first = new PayrollContextSnapshotEntity();
        first.setSnapshotTypeCode("PRESENCE");
        first.setSourceVerticalCode("EMPLOYEE");
        first.setSourceBusinessKeyJson("{\"presenceNumber\":1}");
        first.setSnapshotPayloadJson("{\"companyCode\":\"ES01\"}");

        PayrollContextSnapshotEntity duplicateType = new PayrollContextSnapshotEntity();
        duplicateType.setSnapshotTypeCode("PRESENCE");
        duplicateType.setSourceVerticalCode("EMPLOYEE");
        duplicateType.setSourceBusinessKeyJson("{\"presenceNumber\":1}");
        duplicateType.setSnapshotPayloadJson("{\"companyCode\":\"ES02\"}");

        payroll.replaceContextSnapshots(List.of(first, duplicateType));

        assertEquals(1, payroll.getContextSnapshots().size());
    }

    private PayrollEntity payrollEntity() {
        PayrollEntity payroll = new PayrollEntity();
        payroll.setRuleSystemCode("ESP");
        payroll.setEmployeeTypeCode("INTERNAL");
        payroll.setEmployeeNumber("EMP001");
        payroll.setPayrollPeriodCode("202501");
        payroll.setPayrollTypeCode("NORMAL");
        payroll.setPresenceNumber(1);
        payroll.setCalculatedAt(Instant.parse("2026-01-31T10:15:00Z"));
        return payroll;
    }
}