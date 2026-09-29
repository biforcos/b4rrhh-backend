package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollConcept;
import com.b4rrhh.payroll.domain.model.PayrollContextSnapshot;
import com.b4rrhh.payroll.domain.model.PayrollSegment;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.model.PayrollWarning;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import org.springframework.data.domain.Page;
import com.b4rrhh.payroll.domain.port.PayrollSearchPage;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PayrollPersistenceAdapter implements PayrollRepository {

    private final SpringDataPayrollRepository springDataPayrollRepository;

    public PayrollPersistenceAdapter(SpringDataPayrollRepository springDataPayrollRepository) {
        this.springDataPayrollRepository = springDataPayrollRepository;
    }

    @Override
    public Optional<Payroll> findByBusinessKey(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode,
            Integer presenceNumber
    ) {
        return springDataPayrollRepository
                .findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumberAndPayrollPeriodCodeAndPayrollTypeCodeAndPresenceNumber(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        payrollPeriodCode,
                        payrollTypeCode,
                        presenceNumber
                )
                .map(this::toDomain);
    }

    @Override
    public PayrollSearchPage findPageByFilters(
            String ruleSystemCode,
            String payrollPeriodCode,
            String employeeNumber,
            PayrollStatus status,
            int page,
            int size) {
        Page<PayrollEntity> result = springDataPayrollRepository
                .findPageByFilters(ruleSystemCode, payrollPeriodCode, employeeNumber, status, PageRequest.of(page, size));
        List<Payroll> items = result.getContent().stream().map(this::toDomain).toList();
        return new PayrollSearchPage(items, page, size, result.getTotalElements(), sharedMonths(items));
    }

    /** Una consulta por página, y ninguna si la página viene vacía (`b4rrhh/frontend#104`). */
    private Set<PayrollSearchPage.Month> sharedMonths(List<Payroll> items) {
        if (items.isEmpty()) {
            return Set.of();
        }
        Set<PayrollSearchPage.Month> onThePage = items.stream()
                .map(PayrollSearchPage.Month::of)
                .collect(Collectors.toSet());
        return springDataPayrollRepository.findMonthsWithMoreThanOnePresence(
                        items.stream().map(Payroll::getEmployeeNumber).collect(Collectors.toSet()),
                        items.stream().map(Payroll::getPayrollPeriodCode).collect(Collectors.toSet()))
                .stream()
                .map(row -> new PayrollSearchPage.Month(
                        (String) row[0], (String) row[1], (String) row[2], (String) row[3], (String) row[4]))
                .filter(onThePage::contains)
                .collect(Collectors.toSet());
    }

    @Override
    public Payroll save(Payroll payroll) {
        PayrollEntity entity = payroll.getId() == null
            ? toNewEntity(payroll)
            : springDataPayrollRepository.findById(payroll.getId())
                .map(existing -> applyMutableFields(existing, payroll))
                .orElseGet(() -> toNewEntity(payroll));
        return toDomain(springDataPayrollRepository.save(entity));
    }

    @Override
    public void deleteById(Long id) {
        springDataPayrollRepository.deleteById(id);
    }

    @Override
    public void flush() {
        springDataPayrollRepository.flush();
    }

    private Payroll toDomain(PayrollEntity entity) {
        return Payroll.rehydrate(
                entity.getId(),
                entity.getRuleSystemCode(),
                entity.getEmployeeTypeCode(),
                entity.getEmployeeNumber(),
                entity.getPayrollPeriodCode(),
                entity.getPayrollTypeCode(),
                entity.getPresenceNumber(),
                entity.getStatus(),
                entity.getStatusReasonCode(),
                entity.getCalculatedAt(),
                entity.getCalculationEngineCode(),
                entity.getCalculationEngineVersion(),
                entity.getRunId(),
                entity.getWarnings().stream()
                    .map(warning -> new PayrollWarning(
                        warning.getId(),
                        entity.getId(),
                        warning.getWarningCode(),
                        warning.getSeverityCode(),
                        warning.getMessage(),
                        warning.getDetailsJson()
                    ))
                    .toList(),
                entity.getConcepts().stream()
                        .sorted(Comparator.comparing(PayrollConceptEntity::getDisplayOrder)
                                .thenComparing(PayrollConceptEntity::getLineNumber))
                        .map(concept -> new PayrollConcept(
                                concept.getLineNumber(),
                                concept.getConceptCode(),
                                concept.getConceptMnemonic(),
                                concept.getConceptLabel(),
                                concept.getAmount(),
                                concept.getQuantity(),
                                concept.getRate(),
                                concept.getConceptNatureCode(),
                                concept.getOriginPeriodCode(),
                                concept.getDisplayOrder(),
                                concept.getMergedStepCount(),
                                concept.getPayslipSectionCode(),
                                concept.getPayslipSubsectionCode()
                        ))
                        .toList(),
                entity.getContextSnapshots().stream()
                        .map(snapshot -> new PayrollContextSnapshot(
                                snapshot.getSnapshotTypeCode(),
                                snapshot.getSourceVerticalCode(),
                                snapshot.getSourceBusinessKeyJson(),
                                snapshot.getSnapshotPayloadJson()
                        ))
                        .toList(),
                entity.getSegments().stream()
                        .map(seg -> new PayrollSegment(seg.getSegmentStart()))
                        .toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PayrollEntity toNewEntity(Payroll payroll) {
        PayrollEntity entity = new PayrollEntity();
        entity.setId(payroll.getId());
        applyScalarFields(entity, payroll);
        // Fuera de applyScalarFields a proposito: la ejecucion que produjo un recibo se escribe al
        // crearlo y no se reescribe despues, ni al invalidarlo ni al validarlo (backend#62).
        entity.setRunId(payroll.getRunId());
        entity.replaceConcepts(payroll.getConcepts().stream().map(this::toConceptEntity).toList());
        entity.replaceContextSnapshots(payroll.getContextSnapshots().stream().map(this::toSnapshotEntity).toList());
        entity.replaceWarnings(payroll.getWarnings().stream().map(this::toWarningEntity).toList());
        entity.replaceSegments(payroll.getSegments().stream().map(this::toSegmentEntity).toList());
        return entity;
    }

    private PayrollEntity applyMutableFields(PayrollEntity entity, Payroll payroll) {
        applyScalarFields(entity, payroll);
        return entity;
    }

    private void applyScalarFields(PayrollEntity entity, Payroll payroll) {
        entity.setRuleSystemCode(payroll.getRuleSystemCode());
        entity.setEmployeeTypeCode(payroll.getEmployeeTypeCode());
        entity.setEmployeeNumber(payroll.getEmployeeNumber());
        entity.setPayrollPeriodCode(payroll.getPayrollPeriodCode());
        entity.setPayrollTypeCode(payroll.getPayrollTypeCode());
        entity.setPresenceNumber(payroll.getPresenceNumber());
        entity.setStatus(payroll.getStatus());
        entity.setStatusReasonCode(payroll.getStatusReasonCode());
        entity.setCalculatedAt(payroll.getCalculatedAt());
        entity.setCalculationEngineCode(payroll.getCalculationEngineCode());
        entity.setCalculationEngineVersion(payroll.getCalculationEngineVersion());
        entity.setCreatedAt(payroll.getCreatedAt());
        entity.setUpdatedAt(payroll.getUpdatedAt());
    }

    private PayrollConceptEntity toConceptEntity(PayrollConcept concept) {
        PayrollConceptEntity entity = new PayrollConceptEntity();
        entity.setLineNumber(concept.getLineNumber());
        entity.setConceptCode(concept.getConceptCode());
        entity.setConceptMnemonic(concept.getConceptMnemonic());
        entity.setConceptLabel(concept.getConceptLabel());
        entity.setAmount(concept.getAmount());
        entity.setQuantity(concept.getQuantity());
        entity.setRate(concept.getRate());
        entity.setConceptNatureCode(concept.getConceptNatureCode());
        entity.setOriginPeriodCode(concept.getOriginPeriodCode());
        entity.setDisplayOrder(concept.getDisplayOrder());
        entity.setMergedStepCount(concept.getMergedStepCount());
        entity.setPayslipSectionCode(concept.getPayslipSectionCode());
        entity.setPayslipSubsectionCode(concept.getPayslipSubsectionCode());
        return entity;
    }

    private PayrollContextSnapshotEntity toSnapshotEntity(PayrollContextSnapshot snapshot) {
        PayrollContextSnapshotEntity entity = new PayrollContextSnapshotEntity();
        entity.setSnapshotTypeCode(snapshot.getSnapshotTypeCode());
        entity.setSourceVerticalCode(snapshot.getSourceVerticalCode());
        entity.setSourceBusinessKeyJson(snapshot.getSourceBusinessKeyJson());
        entity.setSnapshotPayloadJson(snapshot.getSnapshotPayloadJson());
        return entity;
    }

    private PayrollSegmentEntity toSegmentEntity(PayrollSegment segment) {
        PayrollSegmentEntity entity = new PayrollSegmentEntity();
        entity.setSegmentStart(segment.segmentStart());
        return entity;
    }

    private PayrollWarningEntity toWarningEntity(PayrollWarning warning) {
        PayrollWarningEntity entity = new PayrollWarningEntity();
        entity.setId(warning.id());
        entity.setWarningCode(warning.warningCode());
        entity.setSeverityCode(warning.severityCode());
        entity.setMessage(warning.message());
        entity.setDetailsJson(warning.detailsJson());
        return entity;
    }
}