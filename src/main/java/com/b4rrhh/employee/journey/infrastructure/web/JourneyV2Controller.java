package com.b4rrhh.employee.journey.infrastructure.web;

import com.b4rrhh.employee.journey.application.usecase.EmployeeJourneyTimelineView;
import com.b4rrhh.employee.journey.application.usecase.GetEmployeeJourneyV2Command;
import com.b4rrhh.employee.journey.application.usecase.GetEmployeeJourneyV2UseCase;
import com.b4rrhh.employee.journey.application.usecase.JourneyEventView;
import com.b4rrhh.employee.journey.infrastructure.web.dto.v2.EmployeeJourneyResponse;
import com.b4rrhh.employee.journey.infrastructure.web.dto.v2.JourneyEmployeeHeader;
import com.b4rrhh.employee.journey.infrastructure.web.dto.v2.JourneyEventResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/journey-v2")
public class JourneyV2Controller {

    private final GetEmployeeJourneyV2UseCase getEmployeeJourneyV2UseCase;

    public JourneyV2Controller(GetEmployeeJourneyV2UseCase getEmployeeJourneyV2UseCase) {
        this.getEmployeeJourneyV2UseCase = getEmployeeJourneyV2UseCase;
    }

    @GetMapping
    public ResponseEntity<EmployeeJourneyResponse> getJourneyV2(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber
    ) {
        EmployeeJourneyTimelineView journey = getEmployeeJourneyV2UseCase.get(
                new GetEmployeeJourneyV2Command(ruleSystemCode, employeeTypeCode, employeeNumber)
        );

        EmployeeJourneyResponse response = new EmployeeJourneyResponse(
                new JourneyEmployeeHeader(
                        journey.employee().ruleSystemCode(),
                        journey.employee().employeeTypeCode(),
                        journey.employee().employeeNumber(),
                        journey.employee().displayName()
                ),
                journey.events().stream()
                        .map(this::toEventResponse)
                        .toList()
        );

        return ResponseEntity.ok(response);
    }

    private JourneyEventResponse toEventResponse(JourneyEventView event) {
        return new JourneyEventResponse(
                event.eventDate(),
                event.eventType().name(),
                event.trackCode().name(),
                event.status().apiValue(),
                event.isCurrent(),
                event.details()
        );
    }
}