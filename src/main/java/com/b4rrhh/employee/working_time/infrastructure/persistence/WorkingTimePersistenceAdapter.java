package com.b4rrhh.employee.working_time.infrastructure.persistence;

import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeDerivedHours;
import com.b4rrhh.employee.working_time.domain.port.WorkingTimeRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class WorkingTimePersistenceAdapter implements WorkingTimeRepository {

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final SpringDataWorkingTimeRepository springDataWorkingTimeRepository;

    public WorkingTimePersistenceAdapter(SpringDataWorkingTimeRepository springDataWorkingTimeRepository) {
        this.springDataWorkingTimeRepository = springDataWorkingTimeRepository;
    }

    @Override
    public Optional<WorkingTime> findByEmployeeIdAndWorkingTimeNumber(Long employeeId, Integer workingTimeNumber) {
        return springDataWorkingTimeRepository
                .findByEmployeeIdAndWorkingTimeNumber(employeeId, workingTimeNumber)
                .map(this::toDomain);
    }

    @Override
    public List<WorkingTime> findByEmployeeIdOrderByStartDate(Long employeeId) {
        return springDataWorkingTimeRepository.findByEmployeeIdOrderByStartDateAsc(employeeId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsOverlappingPeriod(Long employeeId, LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveEndDate = endDate == null ? MAX_DATE : endDate;
        return springDataWorkingTimeRepository.existsOverlappingPeriod(
                employeeId,
                startDate,
                effectiveEndDate,
                MAX_DATE
        );
    }

    @Override
    public boolean existsOverlappingPeriodExcluding(Long employeeId, LocalDate startDate, LocalDate endDate, Integer excludeWorkingTimeNumber) {
        LocalDate effectiveEndDate = endDate == null ? MAX_DATE : endDate;
        return springDataWorkingTimeRepository.existsOverlappingPeriodExcluding(
                employeeId,
                startDate,
                effectiveEndDate,
                MAX_DATE,
                excludeWorkingTimeNumber
        );
    }

    @Override
    public Optional<Integer> findMaxWorkingTimeNumberByEmployeeId(Long employeeId) {
        return Optional.ofNullable(springDataWorkingTimeRepository.findMaxWorkingTimeNumberByEmployeeId(employeeId));
    }

    @Override
    public WorkingTime save(WorkingTime workingTime) {
        WorkingTimeEntity saved = springDataWorkingTimeRepository.save(toEntity(workingTime));
        return toDomain(saved);
    }

    @Override
    public void delete(WorkingTime workingTime) {
        springDataWorkingTimeRepository.deleteById(workingTime.getId());
    }

    private WorkingTime toDomain(WorkingTimeEntity entity) {
        return WorkingTime.rehydrate(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getWorkingTimeNumber(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getWorkingTimePercentage(),
                new WorkingTimeDerivedHours(
                        entity.getWeeklyHours(),
                        entity.getDailyHours(),
                        entity.getMonthlyHours()
                ),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private WorkingTimeEntity toEntity(WorkingTime workingTime) {
        WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setId(workingTime.getId());
        entity.setEmployeeId(workingTime.getEmployeeId());
        entity.setWorkingTimeNumber(workingTime.getWorkingTimeNumber());
        entity.setStartDate(workingTime.getStartDate());
        entity.setEndDate(workingTime.getEndDate());
        entity.setWorkingTimePercentage(workingTime.getWorkingTimePercentage());
        entity.setWeeklyHours(workingTime.getWeeklyHours());
        entity.setDailyHours(workingTime.getDailyHours());
        entity.setMonthlyHours(workingTime.getMonthlyHours());
        entity.setCreatedAt(workingTime.getCreatedAt());
        entity.setUpdatedAt(workingTime.getUpdatedAt());
        return entity;
    }
}