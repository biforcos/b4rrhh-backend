package com.b4rrhh.employee.extra_payment_regime.infrastructure.persistence;

import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegime;
import com.b4rrhh.employee.extra_payment_regime.domain.port.ExtraPaymentRegimeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ExtraPaymentRegimePersistenceAdapter implements ExtraPaymentRegimeRepository {

    private final SpringDataExtraPaymentRegimeRepository springDataExtraPaymentRegimeRepository;

    public ExtraPaymentRegimePersistenceAdapter(
            SpringDataExtraPaymentRegimeRepository springDataExtraPaymentRegimeRepository
    ) {
        this.springDataExtraPaymentRegimeRepository = springDataExtraPaymentRegimeRepository;
    }

    @Override
    public Optional<ExtraPaymentRegime> findByEmployeeIdAndExtraPaymentRegimeNumber(
            Long employeeId,
            Integer extraPaymentRegimeNumber
    ) {
        return springDataExtraPaymentRegimeRepository
                .findByEmployeeIdAndExtraPaymentRegimeNumber(employeeId, extraPaymentRegimeNumber)
                .map(this::toDomain);
    }

    @Override
    public List<ExtraPaymentRegime> findByEmployeeIdOrderByStartDate(Long employeeId) {
        return springDataExtraPaymentRegimeRepository.findByEmployeeIdOrderByStartDateAsc(employeeId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Integer> findMaxExtraPaymentRegimeNumberByEmployeeId(Long employeeId) {
        return Optional.ofNullable(
                springDataExtraPaymentRegimeRepository.findMaxExtraPaymentRegimeNumberByEmployeeId(employeeId));
    }

    @Override
    public ExtraPaymentRegime save(ExtraPaymentRegime extraPaymentRegime) {
        ExtraPaymentRegimeEntity saved = springDataExtraPaymentRegimeRepository.save(toEntity(extraPaymentRegime));
        return toDomain(saved);
    }

    @Override
    public void delete(ExtraPaymentRegime extraPaymentRegime) {
        springDataExtraPaymentRegimeRepository.deleteById(extraPaymentRegime.getId());
    }

    private ExtraPaymentRegime toDomain(ExtraPaymentRegimeEntity entity) {
        return ExtraPaymentRegime.rehydrate(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getExtraPaymentRegimeNumber(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getProrated(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ExtraPaymentRegimeEntity toEntity(ExtraPaymentRegime extraPaymentRegime) {
        ExtraPaymentRegimeEntity entity = new ExtraPaymentRegimeEntity();
        entity.setId(extraPaymentRegime.getId());
        entity.setEmployeeId(extraPaymentRegime.getEmployeeId());
        entity.setExtraPaymentRegimeNumber(extraPaymentRegime.getExtraPaymentRegimeNumber());
        entity.setStartDate(extraPaymentRegime.getStartDate());
        entity.setEndDate(extraPaymentRegime.getEndDate());
        entity.setProrated(extraPaymentRegime.isProrated());
        entity.setCreatedAt(extraPaymentRegime.getCreatedAt());
        entity.setUpdatedAt(extraPaymentRegime.getUpdatedAt());
        return entity;
    }
}
