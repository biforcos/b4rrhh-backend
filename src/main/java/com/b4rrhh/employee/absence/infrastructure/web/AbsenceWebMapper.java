package com.b4rrhh.employee.absence.infrastructure.web;

import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.infrastructure.web.dto.AbsenceResponse;
import org.springframework.stereotype.Component;

@Component
public class AbsenceWebMapper {

    public AbsenceResponse toResponse(Absence absence) {
        return new AbsenceResponse(
            absence.getAbsenceTypeCode(),
            absence.getStartDate(),
            minutesToHHmm(absence.getStartTime()),
            absence.getEndDate(),
            absence.getEndTime() != null ? minutesToHHmm(absence.getEndTime()) : null,
            absence.getCreatedAt(),
            absence.getUpdatedAt()
        );
    }

    public int parseHHmmToMinutes(String hhMm) {
        String[] parts = hhMm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    public int parsePathTimeToMinutes(String hhmm) {
        int hours = Integer.parseInt(hhmm.substring(0, 2));
        int minutes = Integer.parseInt(hhmm.substring(2, 4));
        return hours * 60 + minutes;
    }

    private String minutesToHHmm(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}
