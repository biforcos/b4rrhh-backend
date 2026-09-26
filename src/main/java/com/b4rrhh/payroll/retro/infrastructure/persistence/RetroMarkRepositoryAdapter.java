package com.b4rrhh.payroll.retro.infrastructure.persistence;

import com.b4rrhh.payroll.retro.domain.model.RetroMark;
import com.b4rrhh.payroll.retro.domain.model.RetroMarkSource;
import com.b4rrhh.payroll.retro.domain.model.RetroMarkStatus;
import com.b4rrhh.payroll.retro.domain.port.RetroMarkRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RetroMarkRepositoryAdapter implements RetroMarkRepository {

    private final SpringDataRetroMarkRepository jpa;

    public RetroMarkRepositoryAdapter(SpringDataRetroMarkRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RetroMark save(RetroMark mark) {
        RetroMarkEntity entity = mark.getId() == null
                ? new RetroMarkEntity()
                : jpa.findById(mark.getId()).orElseGet(RetroMarkEntity::new);
        aplicar(mark, entity);
        return aDominio(jpa.save(entity));
    }

    @Override
    public Optional<RetroMark> findById(Long id) {
        return jpa.findById(id).map(RetroMarkRepositoryAdapter::aDominio);
    }

    @Override
    public List<RetroMark> findByEmployee(
            String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        return jpa.findByEmployee(ruleSystemCode, employeeTypeCode, employeeNumber)
                .stream().map(RetroMarkRepositoryAdapter::aDominio).toList();
    }

    @Override
    public List<RetroMark> findActiveByEmployee(
            String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        return jpa.findByEmployeeAndStatus(
                        ruleSystemCode, employeeTypeCode, employeeNumber, RetroMarkStatus.ACTIVE)
                .stream().map(RetroMarkRepositoryAdapter::aDominio).toList();
    }

    private static void aplicar(RetroMark mark, RetroMarkEntity entity) {
        entity.setRuleSystemCode(mark.getRuleSystemCode());
        entity.setEmployeeTypeCode(mark.getEmployeeTypeCode());
        entity.setEmployeeNumber(mark.getEmployeeNumber());
        entity.setPresenceNumber(mark.getPresenceNumber());
        entity.setFromPeriodCode(mark.getFromPeriodCode());
        entity.setStatus(mark.getStatus());
        entity.setCreatedAt(mark.getCreatedAt());
        entity.setSourceVerticalCode(mark.getSource().verticalCode());
        entity.setSourceTable(mark.getSource().table());
        entity.setSourceRowId(mark.getSource().rowId());
        entity.setSourceRowKey(mark.getSource().rowKey());
        entity.setDiscardedAt(mark.getDiscardedAt());
        entity.setDiscardedBy(mark.getDiscardedBy());
        entity.setDiscardReason(mark.getDiscardReason());
        entity.setConsumedAt(mark.getConsumedAt());
        entity.setConsumedPeriodCode(mark.getConsumedPeriodCode());
        entity.setConsumedRunId(mark.getConsumedRunId());
    }

    private static RetroMark aDominio(RetroMarkEntity e) {
        return RetroMark.rehydrate(
                e.getId(),
                e.getRuleSystemCode(),
                e.getEmployeeTypeCode(),
                e.getEmployeeNumber(),
                e.getPresenceNumber(),
                e.getFromPeriodCode(),
                e.getStatus(),
                e.getCreatedAt(),
                new RetroMarkSource(e.getSourceVerticalCode(), e.getSourceTable(),
                        e.getSourceRowId(), e.getSourceRowKey()),
                e.getDiscardedAt(),
                e.getDiscardedBy(),
                e.getDiscardReason(),
                e.getConsumedAt(),
                e.getConsumedPeriodCode(),
                e.getConsumedRunId());
    }
}
