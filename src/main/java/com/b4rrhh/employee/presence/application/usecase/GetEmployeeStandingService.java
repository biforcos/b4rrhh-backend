package com.b4rrhh.employee.presence.application.usecase;

import com.b4rrhh.employee.presence.domain.model.EmployeeStanding;
import com.b4rrhh.employee.presence.domain.model.PresencePeriod;
import com.b4rrhh.employee.presence.domain.port.PresenceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class GetEmployeeStandingService implements GetEmployeeStandingUseCase {

    private final PresenceRepository presenceRepository;

    public GetEmployeeStandingService(PresenceRepository presenceRepository) {
        this.presenceRepository = presenceRepository;
    }

    @Override
    public EmployeeStanding standingOf(Long employeeId, LocalDate date) {
        LocalDate on = date != null ? date : LocalDate.now();
        return EmployeeStanding.on(on, presenceRepository.findByEmployeeIdOrderByStartDate(employeeId).stream()
                .map(presence -> new PresencePeriod(presence.getStartDate(), presence.getEndDate()))
                .toList());
    }
}
