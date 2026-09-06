package com.b4rrhh.employee.cost_center.infrastructure.persistence;

import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class CostCenterPersistenceAdapter implements CostCenterRepository {

    private final SpringDataCostCenterRepository springDataCostCenterRepository;

    public CostCenterPersistenceAdapter(SpringDataCostCenterRepository springDataCostCenterRepository) {
        this.springDataCostCenterRepository = springDataCostCenterRepository;
    }

    @Override
    public List<CostCenterAllocation> findByEmployeeIdOrderByStartDate(Long employeeId) {
        return springDataCostCenterRepository
                .findByEmployeeIdOrderByStartDateAsc(employeeId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<CostCenterAllocation> findActiveAtDate(Long employeeId, LocalDate date) {
        return springDataCostCenterRepository
                .findActiveAtDate(employeeId, date)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<CostCenterAllocation> findByEmployeeIdAndStartDate(Long employeeId, LocalDate startDate) {
        return springDataCostCenterRepository
                .findByEmployeeIdAndStartDate(employeeId, startDate)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void saveAll(List<CostCenterAllocation> allocations) {
        springDataCostCenterRepository.saveAll(
                allocations.stream().map(this::toEntity).toList()
        );
    }

    @Override
    @Transactional
    public void closeAllForWindow(Long employeeId, LocalDate windowStartDate, LocalDate closeDate) {
        springDataCostCenterRepository.closeAllOpenForWindow(employeeId, windowStartDate, closeDate);
    }

    @Override
    @Transactional
    public void adjustWindowEndDate(Long employeeId, LocalDate windowStartDate, LocalDate endDate) {
        springDataCostCenterRepository.setEndDateForWindow(employeeId, windowStartDate, endDate);
    }

    @Override
    @Transactional
    public void deleteAllForWindow(Long employeeId, LocalDate windowStartDate) {
        springDataCostCenterRepository.deleteAllForWindow(employeeId, windowStartDate);
    }

    private CostCenterAllocation toDomain(CostCenterEntity entity) {
        return new CostCenterAllocation(
                entity.getEmployeeId(),
                entity.getCostCenterCode(),
                entity.getAllocationPercentage(),
                entity.getStartDate(),
                entity.getEndDate()
        );
    }

    private CostCenterEntity toEntity(CostCenterAllocation costCenterAllocation) {
        CostCenterEntity entity = new CostCenterEntity();
        entity.setEmployeeId(costCenterAllocation.getEmployeeId());
        entity.setCostCenterCode(costCenterAllocation.getCostCenterCode());
        entity.setAllocationPercentage(costCenterAllocation.getAllocationPercentage());
        entity.setStartDate(costCenterAllocation.getStartDate());
        entity.setEndDate(costCenterAllocation.getEndDate());
        return entity;
    }
}
