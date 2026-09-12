package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollConcept;
import com.b4rrhh.payroll.domain.model.PayrollContextSnapshot;
import com.b4rrhh.payroll.domain.model.PayrollSegment;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.model.PayrollWarning;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
    public List<Payroll> findByFilters(String ruleSystemCode, String payrollPeriodCode, String employeeNumber, PayrollStatus status) {
        return springDataPayrollRepository
                .findByFilters(ruleSystemCode, payrollPeriodCode, employeeNumber, status, PageRequest.of(0, 500))
                .stream()
                .map(this::toDomain)
                .toList();
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
                                concept.getConceptLabel(),
                                concept.getAmount(),
                                concept.getQuantity(),
                                concept.getRate(),
                                concept.getConceptNatureCode(),
                                concept.getOriginPeriodCode(),
                                concept.getDisplayOrder()
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
        entity.setConceptLabel(concept.getConceptLabel());
        entity.setAmount(concept.getAmount());
        entity.setQuantity(concept.getQuantity());
        entity.setRate(concept.getRate());
        entity.setConceptNatureCode(concept.getConceptNatureCode());
        entity.setOriginPeriodCode(concept.getOriginPeriodCode());
        entity.setDisplayOrder(concept.getDisplayOrder());
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