package com.b4rrhh.employee.working_time.infrastructure.web;

import com.b4rrhh.employee.working_time.application.usecase.CloseWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CloseWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.DeleteWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.DeleteWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.GetWorkingTimeByBusinessKeyCommand;
import com.b4rrhh.employee.working_time.application.usecase.GetWorkingTimeByBusinessKeyUseCase;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesCommand;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesUseCase;
import com.b4rrhh.employee.working_time.application.usecase.PlanWorkingTimeChangeCommand;
import com.b4rrhh.employee.working_time.application.usecase.PlanWorkingTimeChangeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.UpdateWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.UpdateWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.infrastructure.web.assembler.WorkingTimeResponseAssembler;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.CloseWorkingTimeRequest;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.CreateWorkingTimeRequest;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.PlanWorkingTimeChangeRequest;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.UpdateWorkingTimeRequest;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.WorkingTimePlanResponse;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.WorkingTimeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final DeleteWorkingTimeUseCase deleteWorkingTimeUseCase;
    private final PlanWorkingTimeChangeUseCase planWorkingTimeChangeUseCase;
    private final WorkingTimeResponseAssembler workingTimeResponseAssembler;

    public WorkingTimeController(
            CreateWorkingTimeUseCase createWorkingTimeUseCase,
            ListEmployeeWorkingTimesUseCase listEmployeeWorkingTimesUseCase,
            GetWorkingTimeByBusinessKeyUseCase getWorkingTimeByBusinessKeyUseCase,
            CloseWorkingTimeUseCase closeWorkingTimeUseCase,
            UpdateWorkingTimeUseCase updateWorkingTimeUseCase,
            DeleteWorkingTimeUseCase deleteWorkingTimeUseCase,
            PlanWorkingTimeChangeUseCase planWorkingTimeChangeUseCase,
            WorkingTimeResponseAssembler workingTimeResponseAssembler
    ) {
        this.createWorkingTimeUseCase = createWorkingTimeUseCase;
        this.listEmployeeWorkingTimesUseCase = listEmployeeWorkingTimesUseCase;
        this.getWorkingTimeByBusinessKeyUseCase = getWorkingTimeByBusinessKeyUseCase;
        this.closeWorkingTimeUseCase = closeWorkingTimeUseCase;
        this.updateWorkingTimeUseCase = updateWorkingTimeUseCase;
        this.deleteWorkingTimeUseCase = deleteWorkingTimeUseCase;
        this.planWorkingTimeChangeUseCase = planWorkingTimeChangeUseCase;
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

    /**
     * What the change would do to the series, without applying it (ADR-057).
     * It is what the screen shows before the user confirms.
     */
    @PostMapping("/plan")
    public ResponseEntity<WorkingTimePlanResponse> plan(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody PlanWorkingTimeChangeRequest request
    ) {
        return ResponseEntity.ok(workingTimeResponseAssembler.toPlanResponse(
                planWorkingTimeChangeUseCase.plan(new PlanWorkingTimeChangeCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.operation(),
                        request.workingTimeNumber(),
                        request.startDate(),
                        request.endDate()
                ))
        ));
    }

    /**
     * Kept while the current screen still closes a working time before adding
     * the next one. To be removed once frontend#43 has migrated the screen to
     * add-with-dates: adding already closes the previous one (ADR-057).
     */
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

    @DeleteMapping("/{workingTimeNumber}")
    public ResponseEntity<Void> delete(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable Integer workingTimeNumber
    ) {
        deleteWorkingTimeUseCase.delete(
                new DeleteWorkingTimeCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        workingTimeNumber
                )
        );

        return ResponseEntity.noContent().build();
    }
}