package com.b4rrhh.employee.contract.infrastructure.rest;

import com.b4rrhh.employee.contract.application.command.CloseContractCommand;
import com.b4rrhh.employee.contract.application.command.CreateContractCommand;
import com.b4rrhh.employee.contract.application.command.GetContractByBusinessKeyCommand;
import com.b4rrhh.employee.contract.application.command.ListEmployeeContractsCommand;
import com.b4rrhh.employee.contract.application.command.ReplaceContractFromDateCommand;
import com.b4rrhh.employee.contract.application.command.UpdateContractCommand;
import com.b4rrhh.employee.contract.application.usecase.CloseContractUseCase;
import com.b4rrhh.employee.contract.application.usecase.CreateContractUseCase;
import com.b4rrhh.employee.contract.application.usecase.GetContractByBusinessKeyUseCase;
import com.b4rrhh.employee.contract.application.usecase.ListEmployeeContractsUseCase;
import com.b4rrhh.employee.contract.application.usecase.ReplaceContractFromDateUseCase;
import com.b4rrhh.employee.contract.application.usecase.UpdateContractUseCase;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.contract.infrastructure.rest.assembler.ContractResponseAssembler;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.CloseContractRequest;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.CreateContractRequest;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.ContractResponse;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.ReplaceContractFromDateRequest;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.UpdateContractRequest;
import org.springframework.format.annotation.DateTimeFormat;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final CloseContractUseCase closeContractUseCase;
        private final ReplaceContractFromDateUseCase replaceContractFromDateUseCase;
        private final ContractResponseAssembler contractResponseAssembler;

    public ContractController(
            CreateContractUseCase createContractUseCase,
            ListEmployeeContractsUseCase listEmployeeContractsUseCase,
            GetContractByBusinessKeyUseCase getContractByBusinessKeyUseCase,
            UpdateContractUseCase updateContractUseCase,
                        CloseContractUseCase closeContractUseCase,
                        ReplaceContractFromDateUseCase replaceContractFromDateUseCase,
                        ContractResponseAssembler contractResponseAssembler
    ) {
        this.createContractUseCase = createContractUseCase;
        this.listEmployeeContractsUseCase = listEmployeeContractsUseCase;
        this.getContractByBusinessKeyUseCase = getContractByBusinessKeyUseCase;
        this.updateContractUseCase = updateContractUseCase;
        this.closeContractUseCase = closeContractUseCase;
                this.replaceContractFromDateUseCase = replaceContractFromDateUseCase;
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
                        request.contractCode(),
                        request.contractSubtypeCode()
                )
        );

        return ResponseEntity.ok(toResponse(ruleSystemCode, updated, language));
    }

    @PostMapping("/{startDate}/close")
    public ResponseEntity<ContractResponse> close(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestBody CloseContractRequest request,
            ResponseLanguage language
    ) {
        Contract closed = closeContractUseCase.close(
                new CloseContractCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        startDate,
                        request.endDate()
                )
        );

        return ResponseEntity.ok(toResponse(ruleSystemCode, closed, language));
    }

    @PostMapping("/replace-from-date")
    public ResponseEntity<ContractResponse> replaceFromDate(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody ReplaceContractFromDateRequest request,
            ResponseLanguage language
    ) {
        Contract replaced = replaceContractFromDateUseCase.replaceFromDate(
                new ReplaceContractFromDateCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.effectiveDate(),
                        request.contractCode(),
                        request.contractSubtypeCode()
                )
        );

                return ResponseEntity.ok(toResponse(ruleSystemCode, replaced, language));
    }

        private ContractResponse toResponse(String ruleSystemCode, Contract contract, ResponseLanguage language) {
                return contractResponseAssembler.toResponse(ruleSystemCode, contract, language);
    }
}
