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

    int parseHHmmToMinutes(String hhMm) {
        if (hhMm == null || !hhMm.matches("\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException("Invalid time format, expected HH:mm but got: " + hhMm);
        }
        String[] parts = hhMm.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        if (hours > 23 || minutes > 59) {
            throw new IllegalArgumentException("Time out of range: " + hhMm);
        }
        return hours * 60 + minutes;
    }

    int parsePathTimeToMinutes(String hhmm) {
        if (hhmm == null || !hhmm.matches("\\d{4}")) {
            throw new IllegalArgumentException("Invalid path time format, expected HHmm but got: " + hhmm);
        }
        int hours = Integer.parseInt(hhmm.substring(0, 2));
        int minutes = Integer.parseInt(hhmm.substring(2, 4));
        if (hours > 23 || minutes > 59) {
            throw new IllegalArgumentException("Time out of range: " + hhmm);
        }
        return hours * 60 + minutes;
    }

    private String minutesToHHmm(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}
