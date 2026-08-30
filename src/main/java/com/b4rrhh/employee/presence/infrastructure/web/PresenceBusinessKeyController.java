package com.b4rrhh.employee.presence.infrastructure.web;

import com.b4rrhh.employee.presence.application.usecase.ClosePresenceCommand;
import com.b4rrhh.employee.presence.application.usecase.ClosePresenceUseCase;
import com.b4rrhh.employee.presence.application.usecase.CreatePresenceCommand;
import com.b4rrhh.employee.presence.application.usecase.CreatePresenceUseCase;
import com.b4rrhh.employee.presence.application.usecase.GetPresenceByBusinessKeyUseCase;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.presence.infrastructure.web.assembler.PresenceResponseAssembler;
import com.b4rrhh.employee.presence.infrastructure.web.dto.ClosePresenceRequest;
import com.b4rrhh.employee.presence.infrastructure.web.dto.CreatePresenceRequest;
import com.b4rrhh.employee.presence.infrastructure.web.dto.PresenceResponse;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/presences")
public class PresenceBusinessKeyController {

    private final CreatePresenceUseCase createPresenceUseCase;
    private final ClosePresenceUseCase closePresenceUseCase;
        private final GetPresenceByBusinessKeyUseCase getPresenceByBusinessKeyUseCase;
    private final ListEmployeePresencesUseCase listEmployeePresencesUseCase;
        private final PresenceResponseAssembler presenceResponseAssembler;

    public PresenceBusinessKeyController(
            CreatePresenceUseCase createPresenceUseCase,
            ClosePresenceUseCase closePresenceUseCase,
            GetPresenceByBusinessKeyUseCase getPresenceByBusinessKeyUseCase,
            ListEmployeePresencesUseCase listEmployeePresencesUseCase,
            PresenceResponseAssembler presenceResponseAssembler
    ) {
        this.createPresenceUseCase = createPresenceUseCase;
        this.closePresenceUseCase = closePresenceUseCase;
        this.getPresenceByBusinessKeyUseCase = getPresenceByBusinessKeyUseCase;
        this.listEmployeePresencesUseCase = listEmployeePresencesUseCase;
        this.presenceResponseAssembler = presenceResponseAssembler;
    }

    @PostMapping
    public ResponseEntity<PresenceResponse> create(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody CreatePresenceRequest request,
            ResponseLanguage language
    ) {
        Presence created = createPresenceUseCase.create(
                new CreatePresenceCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.companyCode(),
                        request.entryReasonCode(),
                        request.exitReasonCode(),
                        request.startDate(),
                        request.endDate()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(presenceResponseAssembler.toResponse(ruleSystemCode, created, language));
    }

    @GetMapping
    public ResponseEntity<List<PresenceResponse>> list(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            ResponseLanguage language
    ) {
        List<PresenceResponse> response = listEmployeePresencesUseCase
                .listByEmployeeBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .stream()
                .map(presence -> presenceResponseAssembler.toResponse(ruleSystemCode, presence, language))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{presenceNumber}")
    public ResponseEntity<PresenceResponse> getByBusinessKey(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable Integer presenceNumber,
            ResponseLanguage language
    ) {
        return getPresenceByBusinessKeyUseCase.getByBusinessKey(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                presenceNumber
        )
                .map(presence -> ResponseEntity.ok(presenceResponseAssembler.toResponse(ruleSystemCode, presence, language)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{presenceNumber}/close")
    public ResponseEntity<PresenceResponse> close(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable Integer presenceNumber,
            @RequestBody ClosePresenceRequest request,
            ResponseLanguage language
    ) {
        Presence closed = closePresenceUseCase.close(
                new ClosePresenceCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        presenceNumber,
                        request.endDate(),
                        request.exitReasonCode()
                )
        );

        return ResponseEntity.ok(presenceResponseAssembler.toResponse(ruleSystemCode, closed, language));
    }
}
