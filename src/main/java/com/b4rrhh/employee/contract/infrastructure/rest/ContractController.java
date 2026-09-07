package com.b4rrhh.employee.contract.infrastructure.rest;

import com.b4rrhh.employee.contract.application.command.CreateContractCommand;
import com.b4rrhh.employee.contract.application.command.DeleteContractCommand;
import com.b4rrhh.employee.contract.application.command.GetContractByBusinessKeyCommand;
import com.b4rrhh.employee.contract.application.command.ListEmployeeContractsCommand;
import com.b4rrhh.employee.contract.application.command.PlanContractChangeCommand;
import com.b4rrhh.employee.contract.application.command.UpdateContractCommand;
import com.b4rrhh.employee.contract.application.usecase.CreateContractUseCase;
import com.b4rrhh.employee.contract.application.usecase.DeleteContractUseCase;
import com.b4rrhh.employee.contract.application.usecase.GetContractByBusinessKeyUseCase;
import com.b4rrhh.employee.contract.application.usecase.ListEmployeeContractsUseCase;
import com.b4rrhh.employee.contract.application.usecase.PlanContractChangeUseCase;
import com.b4rrhh.employee.contract.application.usecase.UpdateContractUseCase;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.contract.infrastructure.rest.assembler.ContractResponseAssembler;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.ContractPlanResponse;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.CreateContractRequest;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.ContractResponse;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.PlanContractChangeRequest;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.UpdateContractRequest;
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
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contracts")
public class ContractController {

    private final CreateContractUseCase createContractUseCase;
    private final ListEmployeeContractsUseCase listEmployeeContractsUseCase;
    private final GetContractByBusinessKeyUseCase getContractByBusinessKeyUseCase;
    private final UpdateContractUseCase updateContractUseCase;
    private final DeleteContractUseCase deleteContractUseCase;
    private final PlanContractChangeUseCase planContractChangeUseCase;
    private final ContractResponseAssembler contractResponseAssembler;

    public ContractController(
            CreateContractUseCase createContractUseCase,
            ListEmployeeContractsUseCase listEmployeeContractsUseCase,
            GetContractByBusinessKeyUseCase getContractByBusinessKeyUseCase,
            UpdateContractUseCase updateContractUseCase,
            DeleteContractUseCase deleteContractUseCase,
            PlanContractChangeUseCase planContractChangeUseCase,
            ContractResponseAssembler contractResponseAssembler
    ) {
        this.createContractUseCase = createContractUseCase;
        this.listEmployeeContractsUseCase = listEmployeeContractsUseCase;
        this.getContractByBusinessKeyUseCase = getContractByBusinessKeyUseCase;
        this.updateContractUseCase = updateContractUseCase;
        this.deleteContractUseCase = deleteContractUseCase;
        this.planContractChangeUseCase = planContractChangeUseCase;
        this.contractResponseAssembler = contractResponseAssembler;
    }

    @PostMapping
    public ResponseEntity<ContractResponse> create(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody CreateContractRequest request,
            ResponseLanguage language
    ) {
        Contract created = createContractUseCase.create(
                new CreateContractCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.contractCode(),
                        request.contractSubtypeCode(),
                        request.startDate(),
                        request.endDate()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(ruleSystemCode, created, language));
    }

    @GetMapping
    public ResponseEntity<List<ContractResponse>> list(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            ResponseLanguage language
    ) {
        List<ContractResponse> response = listEmployeeContractsUseCase
                .listByEmployeeBusinessKey(new ListEmployeeContractsCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber
                ))
                .stream()
                .map(contract -> toResponse(ruleSystemCode, contract, language))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{startDate}")
    public ResponseEntity<ContractResponse> getByBusinessKey(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            ResponseLanguage language
    ) {
        Contract contract = getContractByBusinessKeyUseCase.getByBusinessKey(
                new GetContractByBusinessKeyCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        startDate
                )
        );

        return ResponseEntity.ok(toResponse(ruleSystemCode, contract, language));
    }

    @PutMapping("/{startDate}")
    public ResponseEntity<ContractResponse> update(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestBody UpdateContractRequest request,
            ResponseLanguage language
    ) {
        Contract updated = updateContractUseCase.update(
                new UpdateContractCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        startDate,
                        request.startDate(),
                        request.endDate(),
                        request.contractCode(),
                        request.contractSubtypeCode()
                )
        );

        return ResponseEntity.ok(toResponse(ruleSystemCode, updated, language));
    }

    /**
     * What the change would do to the series, without applying it (ADR-057).
     * It is what the screen shows before the user confirms.
     */
    @PostMapping("/plan")
    public ResponseEntity<ContractPlanResponse> plan(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody PlanContractChangeRequest request
    ) {
        return ResponseEntity.ok(contractResponseAssembler.toPlanResponse(
                planContractChangeUseCase.plan(new PlanContractChangeCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.operation(),
                        request.contractStartDate(),
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
        deleteContractUseCase.delete(
                new DeleteContractCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        startDate
                )
        );

        return ResponseEntity.noContent().build();
    }

    private ContractResponse toResponse(String ruleSystemCode, Contract contract, ResponseLanguage language) {
        return contractResponseAssembler.toResponse(ruleSystemCode, contract, language);
    }
}
