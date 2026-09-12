package com.b4rrhh.employee.workcenter.infrastructure.persistence;

import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class WorkCenterPersistenceAdapter implements WorkCenterRepository {

    private final SpringDataWorkCenterRepository springDataWorkCenterRepository;

    public WorkCenterPersistenceAdapter(SpringDataWorkCenterRepository springDataWorkCenterRepository) {
        this.springDataWorkCenterRepository = springDataWorkCenterRepository;
    }

    @Override
    public Optional<WorkCenter> findByEmployeeIdAndWorkCenterAssignmentNumber(Long employeeId, Integer workCenterAssignmentNumber) {
        return springDataWorkCenterRepository
                .findByEmployeeIdAndWorkCenterAssignmentNumber(employeeId, workCenterAssignmentNumber)
                .map(this::toDomain);
    }

    @Override
    public List<WorkCenter> findByEmployeeIdOrderByStartDate(Long employeeId) {
        return springDataWorkCenterRepository
                .findByEmployeeIdOrderByStartDateAsc(employeeId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Integer> findMaxWorkCenterAssignmentNumberByEmployeeId(Long employeeId) {
        return Optional.ofNullable(springDataWorkCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(employeeId));
    }

    @Override
    public WorkCenter save(WorkCenter workCenter) {
        WorkCenterEntity entity = toEntity(workCenter);
        WorkCenterEntity saved = springDataWorkCenterRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void delete(WorkCenter workCenter) {
        springDataWorkCenterRepository.delete(toEntity(workCenter));
    }

    private WorkCenter toDomain(WorkCenterEntity entity) {
        return new WorkCenter(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getWorkCenterAssignmentNumber(),
                entity.getWorkCenterCode(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private WorkCenterEntity toEntity(WorkCenter workCenter) {
        WorkCenterEntity entity = new WorkCenterEntity();
        entity.setId(workCenter.getId());
        entity.setEmployeeId(workCenter.getEmployeeId());
        entity.setWorkCenterAssignmentNumber(workCenter.getWorkCenterAssignmentNumber());
        entity.setWorkCenterCode(workCenter.getWorkCenterCode());
        entity.setStartDate(workCenter.getStartDate());
        entity.setEndDate(workCenter.getEndDate());
        entity.setCreatedAt(workCenter.getCreatedAt());
        entity.setUpdatedAt(workCenter.getUpdatedAt());
        return entity;
    }
}