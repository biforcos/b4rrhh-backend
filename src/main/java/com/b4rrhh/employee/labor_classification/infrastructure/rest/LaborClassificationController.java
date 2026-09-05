package com.b4rrhh.employee.labor_classification.infrastructure.rest;

import com.b4rrhh.employee.labor_classification.application.command.CloseLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.command.CreateLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.command.DeleteLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.command.GetLaborClassificationByBusinessKeyCommand;
import com.b4rrhh.employee.labor_classification.application.command.ListEmployeeLaborClassificationsCommand;
import com.b4rrhh.employee.labor_classification.application.command.PlanLaborClassificationChangeCommand;
import com.b4rrhh.employee.labor_classification.application.command.ReplaceLaborClassificationFromDateCommand;
import com.b4rrhh.employee.labor_classification.application.command.UpdateLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.usecase.CloseLaborClassificationUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.CreateLaborClassificationUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.DeleteLaborClassificationUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.GetLaborClassificationByBusinessKeyUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.ListEmployeeLaborClassificationsUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.PlanLaborClassificationChangeUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.ReplaceLaborClassificationFromDateUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.UpdateLaborClassificationUseCase;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.infrastructure.rest.assembler.LaborClassificationResponseAssembler;
import com.b4rrhh.employee.labor_classification.infrastructure.rest.dto.CloseLaborClassificationRequest;
import com.b4rrhh.employee.labor_classification.infrastructure.rest.dto.CreateLaborClassificationRequest;
import com.b4rrhh.employee.labor_classification.infrastructure.rest.dto.LaborClassificationPlanResponse;
import com.b4rrhh.employee.labor_classification.infrastructure.rest.dto.LaborClassificationResponse;
import com.b4rrhh.employee.labor_classification.infrastructure.rest.dto.PlanLaborClassificationChangeRequest;
import com.b4rrhh.employee.labor_classification.infrastructure.rest.dto.ReplaceLaborClassificationFromDateRequest;
import com.b4rrhh.employee.labor_classification.infrastructure.rest.dto.UpdateLaborClassificationRequest;
import org.springframework.format.annotation.DateTimeFormat;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/labor-classifications")
public class LaborClassificationController {

    private final CreateLaborClassificationUseCase createLaborClassificationUseCase;
    private final ListEmployeeLaborClassificationsUseCase listEmployeeLaborClassificationsUseCase;
    private final GetLaborClassificationByBusinessKeyUseCase getLaborClassificationByBusinessKeyUseCase;
    private final UpdateLaborClassificationUseCase updateLaborClassificationUseCase;
    private final CloseLaborClassificationUseCase closeLaborClassificationUseCase;
    private final ReplaceLaborClassificationFromDateUseCase replaceLaborClassificationFromDateUseCase;
    private final DeleteLaborClassificationUseCase deleteLaborClassificationUseCase;
    private final PlanLaborClassificationChangeUseCase planLaborClassificationChangeUseCase;
    private final LaborClassificationResponseAssembler laborClassificationResponseAssembler;

    public LaborClassificationController(
            CreateLaborClassificationUseCase createLaborClassificationUseCase,
            ListEmployeeLaborClassificationsUseCase listEmployeeLaborClassificationsUseCase,
            GetLaborClassificationByBusinessKeyUseCase getLaborClassificationByBusinessKeyUseCase,
            UpdateLaborClassificationUseCase updateLaborClassificationUseCase,
            CloseLaborClassificationUseCase closeLaborClassificationUseCase,
            ReplaceLaborClassificationFromDateUseCase replaceLaborClassificationFromDateUseCase,
            DeleteLaborClassificationUseCase deleteLaborClassificationUseCase,
            PlanLaborClassificationChangeUseCase planLaborClassificationChangeUseCase,
            LaborClassificationResponseAssembler laborClassificationResponseAssembler
    ) {
        this.createLaborClassificationUseCase = createLaborClassificationUseCase;
        this.listEmployeeLaborClassificationsUseCase = listEmployeeLaborClassificationsUseCase;
        this.getLaborClassificationByBusinessKeyUseCase = getLaborClassificationByBusinessKeyUseCase;
        this.updateLaborClassificationUseCase = updateLaborClassificationUseCase;
        this.closeLaborClassificationUseCase = closeLaborClassificationUseCase;
        this.replaceLaborClassificationFromDateUseCase = replaceLaborClassificationFromDateUseCase;
        this.deleteLaborClassificationUseCase = deleteLaborClassificationUseCase;
        this.planLaborClassificationChangeUseCase = planLaborClassificationChangeUseCase;
        this.laborClassificationResponseAssembler = laborClassificationResponseAssembler;
    }

    @PostMapping
    public ResponseEntity<LaborClassificationResponse> create(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody CreateLaborClassificationRequest request,
            ResponseLanguage language
    ) {
        LaborClassification created = createLaborClassificationUseCase.create(
                new CreateLaborClassificationCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.agreementCode(),
                        request.agreementCategoryCode(),
                        request.startDate(),
                        request.endDate()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(laborClassificationResponseAssembler.toResponse(ruleSystemCode, created, language));
    }

    @GetMapping
    public ResponseEntity<List<LaborClassificationResponse>> list(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            ResponseLanguage language
    ) {
        List<LaborClassificationResponse> response = laborClassificationResponseAssembler.toResponseList(
                ruleSystemCode,
                listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(new ListEmployeeLaborClassificationsCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber
                ))
        , language);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{startDate}")
    public ResponseEntity<LaborClassificationResponse> getByBusinessKey(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            ResponseLanguage language
    ) {
        LaborClassification laborClassification = getLaborClassificationByBusinessKeyUseCase.getByBusinessKey(
                new GetLaborClassificationByBusinessKeyCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        startDate
                )
        );

        return ResponseEntity.ok(laborClassificationResponseAssembler.toResponse(ruleSystemCode, laborClassification, language));
    }

    @PutMapping("/{startDate}")
    public ResponseEntity<LaborClassificationResponse> update(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestBody UpdateLaborClassificationRequest request,
            ResponseLanguage language
    ) {
        LaborClassification updated = updateLaborClassificationUseCase.update(
                new UpdateLaborClassificationCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        startDate,
                        request.startDate(),
                        request.endDate(),
                        request.agreementCode(),
                        request.agreementCategoryCode()
                )
        );

        return ResponseEntity.ok(laborClassificationResponseAssembler.toResponse(ruleSystemCode, updated, language));
    }

    /**
     * Kept while the labor classification screen still closes an occurrence
     * before adding the next one. To be removed once the screen has migrated:
     * adding an occurrence already closes the one in force the day before
     * (ADR-057).
     */
    @Deprecated
    @PostMapping("/{startDate}/close")
    public ResponseEntity<LaborClassificationResponse> close(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestBody CloseLaborClassificationRequest request,
            ResponseLanguage language
    ) {
        LaborClassification closed = closeLaborClassificationUseCase.close(
                new CloseLaborClassificationCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        startDate,
                        request.endDate()
                )
        );

        return ResponseEntity.ok(laborClassificationResponseAssembler.toResponse(ruleSystemCode, closed, language));
    }

    /**
     * Kept while the labor classification screen and the workforce loader
     * still call it. ADR-057 retires replace-from-date as a model: it is an add
     * whose end date is the tail of the occurrence in force on the effective
     * date.
     */
    @Deprecated
    @PostMapping("/replace-from-date")
    public ResponseEntity<LaborClassificationResponse> replaceFromDate(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody ReplaceLaborClassificationFromDateRequest request,
            ResponseLanguage language
    ) {
        LaborClassification replaced = replaceLaborClassificationFromDateUseCase.replaceFromDate(
                new ReplaceLaborClassificationFromDateCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.effectiveDate(),
                        request.agreementCode(),
                        request.agreementCategoryCode()
                )
        );

        return ResponseEntity.ok(laborClassificationResponseAssembler.toResponse(ruleSystemCode, replaced, language));
    }

    /**
     * What the change would do to the series, without applying it (ADR-057).
     * It is what the screen shows before the user confirms.
     */
    @PostMapping("/plan")
    public ResponseEntity<LaborClassificationPlanResponse> plan(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody PlanLaborClassificationChangeRequest request
    ) {
        return ResponseEntity.ok(laborClassificationResponseAssembler.toPlanResponse(
                planLaborClassificationChangeUseCase.plan(new PlanLaborClassificationChangeCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.operation(),
                        request.laborClassificationStartDate(),
                        request.startDate(),
                        request.endDate()
                ))
        ));
    }

    @DeleteMapping("/{startDate}")
    public ResponseEntity<Void> delete(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        deleteLaborClassificationUseCase.delete(
                new DeleteLaborClassificationCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        startDate
                )
        );

        return ResponseEntity.noContent().build();
    }
}
