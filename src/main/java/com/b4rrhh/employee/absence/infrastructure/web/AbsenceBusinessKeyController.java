package com.b4rrhh.employee.absence.infrastructure.web;

import com.b4rrhh.employee.absence.application.usecase.DeleteAbsenceCommand;
import com.b4rrhh.employee.absence.application.usecase.DeleteAbsenceUseCase;
import com.b4rrhh.employee.absence.application.usecase.GetAbsenceByBusinessKeyCommand;
import com.b4rrhh.employee.absence.application.usecase.GetAbsenceByBusinessKeyUseCase;
import com.b4rrhh.employee.absence.application.usecase.ListEmployeeAbsencesCommand;
import com.b4rrhh.employee.absence.application.usecase.ListEmployeeAbsencesUseCase;
import com.b4rrhh.employee.absence.application.usecase.UpsertAbsenceCommand;
import com.b4rrhh.employee.absence.application.usecase.UpsertAbsenceUseCase;
import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.infrastructure.web.dto.AbsenceResponse;
import com.b4rrhh.employee.absence.infrastructure.web.dto.UpsertAbsenceRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/absences")
public class AbsenceBusinessKeyController {

    private final UpsertAbsenceUseCase upsert;
    private final GetAbsenceByBusinessKeyUseCase get;
    private final ListEmployeeAbsencesUseCase list;
    private final DeleteAbsenceUseCase delete;
    private final AbsenceWebMapper mapper;

    public AbsenceBusinessKeyController(UpsertAbsenceUseCase upsert,
                                         GetAbsenceByBusinessKeyUseCase get,
                                         ListEmployeeAbsencesUseCase list,
                                         DeleteAbsenceUseCase delete,
                                         AbsenceWebMapper mapper) {
        this.upsert = upsert;
        this.get = get;
        this.list = list;
        this.delete = delete;
        this.mapper = mapper;
    }

    @PutMapping("/{absenceTypeCode}/{startDate}")
    public AbsenceResponse upsertDayMode(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String absenceTypeCode,
            @PathVariable LocalDate startDate,
            @RequestBody UpsertAbsenceRequest request) {
        return doUpsert(ruleSystemCode, employeeTypeCode, employeeNumber,
            absenceTypeCode, startDate, 0, request);
    }

    @PutMapping("/{absenceTypeCode}/{startDate}/{startTime}")
    public AbsenceResponse upsertHourMode(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String absenceTypeCode,
            @PathVariable LocalDate startDate,
            @PathVariable String startTime,
            @RequestBody UpsertAbsenceRequest request) {
        return doUpsert(ruleSystemCode, employeeTypeCode, employeeNumber,
            absenceTypeCode, startDate, mapper.parsePathTimeToMinutes(startTime), request);
    }

    private AbsenceResponse doUpsert(String rs, String et, String en, String typeCode,
                                      LocalDate startDate, int startTimeMinutes,
                                      UpsertAbsenceRequest request) {
        Integer endTimeMinutes = request.endTime() != null
            ? mapper.parseHHmmToMinutes(request.endTime()) : null;
        UpsertAbsenceCommand cmd = new UpsertAbsenceCommand(rs, et, en, typeCode,
            startDate, startTimeMinutes, request.endDate(), endTimeMinutes);
        Absence absence = upsert.upsert(cmd);
        return mapper.toResponse(absence);
    }

    @GetMapping("/{absenceTypeCode}/{startDate}")
    public AbsenceResponse getDayMode(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String absenceTypeCode,
            @PathVariable LocalDate startDate) {
        return doGet(ruleSystemCode, employeeTypeCode, employeeNumber, absenceTypeCode, startDate, 0);
    }

    @GetMapping("/{absenceTypeCode}/{startDate}/{startTime}")
    public AbsenceResponse getHourMode(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String absenceTypeCode,
            @PathVariable LocalDate startDate,
            @PathVariable String startTime) {
        return doGet(ruleSystemCode, employeeTypeCode, employeeNumber, absenceTypeCode,
            startDate, mapper.parsePathTimeToMinutes(startTime));
    }

    private AbsenceResponse doGet(String rs, String et, String en, String typeCode,
                                   LocalDate startDate, int startTimeMinutes) {
        Absence absence = get.getByBusinessKey(
            new GetAbsenceByBusinessKeyCommand(rs, et, en, typeCode, startDate, startTimeMinutes));
        return mapper.toResponse(absence);
    }

    @GetMapping
    public List<AbsenceResponse> listAbsences(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber) {
        return list.listByEmployeeBusinessKey(
                new ListEmployeeAbsencesCommand(ruleSystemCode, employeeTypeCode, employeeNumber))
            .stream().map(mapper::toResponse).toList();
    }

    @DeleteMapping("/{absenceTypeCode}/{startDate}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDayMode(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String absenceTypeCode,
            @PathVariable LocalDate startDate) {
        doDelete(ruleSystemCode, employeeTypeCode, employeeNumber, absenceTypeCode, startDate, 0);
    }

    @DeleteMapping("/{absenceTypeCode}/{startDate}/{startTime}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHourMode(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String absenceTypeCode,
            @PathVariable LocalDate startDate,
            @PathVariable String startTime) {
        doDelete(ruleSystemCode, employeeTypeCode, employeeNumber, absenceTypeCode,
            startDate, mapper.parsePathTimeToMinutes(startTime));
    }

    private void doDelete(String rs, String et, String en, String typeCode,
                           LocalDate startDate, int startTimeMinutes) {
        delete.delete(new DeleteAbsenceCommand(rs, et, en, typeCode, startDate, startTimeMinutes));
    }
}
