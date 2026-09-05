package com.b4rrhh.employee.labor_classification.infrastructure.persistence;

import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.domain.port.LaborClassificationRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class LaborClassificationPersistenceAdapter implements LaborClassificationRepository {

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final SpringDataLaborClassificationRepository springDataLaborClassificationRepository;

    public LaborClassificationPersistenceAdapter(
            SpringDataLaborClassificationRepository springDataLaborClassificationRepository
    ) {
        this.springDataLaborClassificationRepository = springDataLaborClassificationRepository;
    }

    @Override
    public Optional<LaborClassification> findByEmployeeIdAndStartDate(Long employeeId, LocalDate startDate) {
        return springDataLaborClassificationRepository
                .findByEmployeeIdAndStartDate(employeeId, startDate)
                .map(this::toDomain);
    }

    @Override
    public List<LaborClassification> findByEmployeeIdOrderByStartDate(Long employeeId) {
        return springDataLaborClassificationRepository
                .findByEmployeeIdOrderByStartDateAsc(employeeId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsOverlappingPeriod(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate excludeStartDateOrNull
    ) {
        LocalDate effectiveEndDate = endDate == null ? MAX_DATE : endDate;
        return springDataLaborClassificationRepository.existsOverlappingPeriod(
                employeeId,
                startDate,
                effectiveEndDate,
                MAX_DATE,
                excludeStartDateOrNull
        );
    }

    @Override
    public void save(LaborClassification laborClassification) {
        springDataLaborClassificationRepository.save(toEntity(laborClassification));
    }

    @Override
    public void update(LaborClassification laborClassification, LocalDate originalStartDate) {
        LaborClassificationEntity entity = springDataLaborClassificationRepository
                .findByEmployeeIdAndStartDate(laborClassification.getEmployeeId(), originalStartDate)
                .orElseThrow(() -> new IllegalStateException(
                        "Labor classification not found for update by functional identity"
                ));

        entity.setAgreementCode(laborClassification.getAgreementCode());
        entity.setAgreementCategoryCode(laborClassification.getAgreementCategoryCode());
        entity.setStartDate(laborClassification.getStartDate());
        entity.setEndDate(laborClassification.getEndDate());
        springDataLaborClassificationRepository.save(entity);
    }

    @Override
    public void delete(LaborClassification laborClassification) {
        LaborClassificationEntity entity = springDataLaborClassificationRepository
                .findByEmployeeIdAndStartDate(laborClassification.getEmployeeId(), laborClassification.getStartDate())
                .orElseThrow(() -> new IllegalStateException(
                        "Labor classification not found for delete by functional identity"
                ));

        springDataLaborClassificationRepository.delete(entity);
    }

    private LaborClassification toDomain(LaborClassificationEntity entity) {
        return new LaborClassification(
                entity.getEmployeeId(),
                entity.getAgreementCode(),
                entity.getAgreementCategoryCode(),
                entity.getStartDate(),
                entity.getEndDate()
        );
    }

    private LaborClassificationEntity toEntity(LaborClassification laborClassification) {
        LaborClassificationEntity entity = new LaborClassificationEntity();
        entity.setEmployeeId(laborClassification.getEmployeeId());
        entity.setAgreementCode(laborClassification.getAgreementCode());
        entity.setAgreementCategoryCode(laborClassification.getAgreementCategoryCode());
        entity.setStartDate(laborClassification.getStartDate());
        entity.setEndDate(laborClassification.getEndDate());
        return entity;
    }
}
