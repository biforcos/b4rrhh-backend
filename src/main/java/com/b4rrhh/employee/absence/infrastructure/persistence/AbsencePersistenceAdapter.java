package com.b4rrhh.employee.absence.infrastructure.persistence;

import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class AbsencePersistenceAdapter implements AbsenceRepository {

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final SpringDataAbsenceRepository springRepo;

    public AbsencePersistenceAdapter(SpringDataAbsenceRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<Absence> findByKey(Long employeeId, String absenceTypeCode,
                                        LocalDate startDate, int startTime) {
        return springRepo.findByEmployeeIdAndAbsenceTypeCodeAndStartDateAndStartTime(
            employeeId, absenceTypeCode, startDate, startTime).map(this::toDomain);
    }

    @Override
    public List<Absence> findByEmployeeIdOrderByStartDateDescStartTimeDesc(Long employeeId) {
        return springRepo.findByEmployeeIdOrderByStartDateDescStartTimeDesc(employeeId)
            .stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsOverlappingAbsence(Long employeeId, LocalDate startDate, LocalDate effectiveEndDate) {
        return springRepo.existsOverlappingAbsence(employeeId, startDate, effectiveEndDate, MAX_DATE);
    }

    @Override
    public boolean existsOverlappingAbsenceExcluding(Long employeeId, LocalDate startDate,
                                                      LocalDate effectiveEndDate, Long excludeId) {
        return springRepo.existsOverlappingAbsenceExcluding(employeeId, startDate, effectiveEndDate, excludeId, MAX_DATE);
    }

    @Override
    public Absence save(Absence absence) {
        AbsenceEntity entity = toEntity(absence);
        return toDomain(springRepo.save(entity));
    }

    @Override
    public void deleteByKey(Long employeeId, String absenceTypeCode, LocalDate startDate, int startTime) {
        springRepo.deleteByEmployeeIdAndAbsenceTypeCodeAndStartDateAndStartTime(
            employeeId, absenceTypeCode, startDate, startTime);
    }

    private Absence toDomain(AbsenceEntity e) {
        return Absence.rehydrate(e.getId(), e.getEmployeeId(), e.getAbsenceTypeCode(),
            e.getStartDate(), e.getStartTime(), e.getEndDate(), e.getEndTime(),
            e.getCreatedAt(), e.getUpdatedAt());
    }

    private AbsenceEntity toEntity(Absence a) {
        AbsenceEntity e = new AbsenceEntity();
        e.setId(a.getId());
        e.setEmployeeId(a.getEmployeeId());
        e.setAbsenceTypeCode(a.getAbsenceTypeCode());
        e.setStartDate(a.getStartDate());
        e.setStartTime(a.getStartTime());
        e.setEndDate(a.getEndDate());
        e.setEndTime(a.getEndTime());
        e.setCreatedAt(a.getCreatedAt());
        e.setUpdatedAt(a.getUpdatedAt());
        return e;
    }
}
