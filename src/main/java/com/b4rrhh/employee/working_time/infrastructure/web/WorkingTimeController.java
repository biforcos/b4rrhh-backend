package com.b4rrhh.employee.working_time.infrastructure.web;

import com.b4rrhh.employee.working_time.application.usecase.CloseWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CloseWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.GetWorkingTimeByBusinessKeyCommand;
import com.b4rrhh.employee.working_time.application.usecase.GetWorkingTimeByBusinessKeyUseCase;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesCommand;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesUseCase;
import com.b4rrhh.employee.working_time.application.usecase.UpdateWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.UpdateWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.infrastructure.web.assembler.WorkingTimeResponseAssembler;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.CloseWorkingTimeRequest;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.CreateWorkingTimeRequest;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.UpdateWorkingTimeRequest;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.WorkingTimeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/working-times")
public class WorkingTimeController {

    private final CreateWorkingTimeUseCase createWorkingTimeUseCase;
    private final ListEmployeeWorkingTimesUseCase listEmployeeWorkingTimesUseCase;
    private final GetWorkingTimeByBusinessKeyUseCase getWorkingTimeByBusinessKeyUseCase;
    private final CloseWorkingTimeUseCase closeWorkingTimeUseCase;
    private final UpdateWorkingTimeUseCase updateWorkingTimeUseCase;
    private final WorkingTimeResponseAssembler workingTimeResponseAssembler;

    public WorkingTimeController(
            CreateWorkingTimeUseCase createWorkingTimeUseCase,
            ListEmployeeWorkingTimesUseCase listEmployeeWorkingTimesUseCase,
            GetWorkingTimeByBusinessKeyUseCase getWorkingTimeByBusinessKeyUseCase,
            CloseWorkingTimeUseCase closeWorkingTimeUseCase,
            UpdateWorkingTimeUseCase updateWorkingTimeUseCase,
            WorkingTimeResponseAssembler workingTimeResponseAssembler
    ) {
        this.createWorkingTimeUseCase = createWorkingTimeUseCase;
        this.listEmployeeWorkingTimesUseCase = listEmployeeWorkingTimesUseCase;
        this.getWorkingTimeByBusinessKeyUseCase = getWorkingTimeByBusinessKeyUseCase;
        this.closeWorkingTimeUseCase = closeWorkingTimeUseCase;
        this.updateWorkingTimeUseCase = updateWorkingTimeUseCase;
        this.workingTimeResponseAssembler = workingTimeResponseAssembler;
    }

    @PostMapping
    public ResponseEntity<WorkingTimeResponse> create(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody CreateWorkingTimeRequest request
    ) {
        WorkingTime created = createWorkingTimeUseCase.create(
                new CreateWorkingTimeCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.getStartDate(),
                        request.getEndDate(),
                        request.getWorkingTimePercentage()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workingTimeResponseAssembler.toResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<WorkingTimeResponse>> list(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber
    ) {
        List<WorkingTimeResponse> response = workingTimeResponseAssembler.toResponseList(
                listEmployeeWorkingTimesUseCase.listByEmployeeBusinessKey(
                        new ListEmployeeWorkingTimesCommand(
                                ruleSystemCode,
                                employeeTypeCode,
                                employeeNumber
                        )
                )
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{workingTimeNumber}")
    public ResponseEntity<WorkingTimeResponse> getByBusinessKey(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable Integer workingTimeNumber
    ) {
        WorkingTime workingTime = getWorkingTimeByBusinessKeyUseCase.getByBusinessKey(
                new GetWorkingTimeByBusinessKeyCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        workingTimeNumber
                )
        );

        return ResponseEntity.ok(workingTimeResponseAssembler.toResponse(workingTime));
    }

    @PostMapping("/{workingTimeNumber}/close")
    public ResponseEntity<WorkingTimeResponse> close(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable Integer workingTimeNumber,
            @RequestBody CloseWorkingTimeRequest request
    ) {
        WorkingTime closed = closeWorkingTimeUseCase.close(
                new CloseWorkingTimeCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        workingTimeNumber,
                        request.getEndDate()
                )
        );

        return ResponseEntity.ok(workingTimeResponseAssembler.toResponse(closed));
    }

    @PutMapping("/{workingTimeNumber}")
    public ResponseEntity<WorkingTimeResponse> update(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable Integer workingTimeNumber,
            @RequestBody UpdateWorkingTimeRequest request
    ) {
        WorkingTime updated = updateWorkingTimeUseCase.update(
                new UpdateWorkingTimeCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        workingTimeNumber,
                        request.startDate(),
                        request.endDate(),
                        request.workingTimePercentage()
                )
        );

        return ResponseEntity.ok(workingTimeResponseAssembler.toResponse(updated));
    }
}