package com.b4rrhh.rulesystem.company.infrastructure.web;

import com.b4rrhh.rulesystem.company.application.usecase.CreateCompanyCommand;
import com.b4rrhh.rulesystem.company.application.usecase.CreateCompanyUseCase;
import com.b4rrhh.rulesystem.company.application.usecase.GetCompanyQuery;
import com.b4rrhh.rulesystem.company.application.usecase.GetCompanyUseCase;
import com.b4rrhh.rulesystem.company.application.usecase.ListCompaniesQuery;
import com.b4rrhh.rulesystem.company.application.usecase.ListCompaniesUseCase;
import com.b4rrhh.rulesystem.company.application.usecase.UpdateCompanyCommand;
import com.b4rrhh.rulesystem.company.application.usecase.UpdateCompanyUseCase;
import com.b4rrhh.rulesystem.company.domain.model.Company;
import com.b4rrhh.rulesystem.company.infrastructure.web.assembler.CompanyResponseAssembler;
import com.b4rrhh.rulesystem.company.infrastructure.web.dto.CompanyAddressRequest;
import com.b4rrhh.rulesystem.company.infrastructure.web.dto.CompanyListItemResponse;
import com.b4rrhh.rulesystem.company.infrastructure.web.dto.CompanyResponse;
import com.b4rrhh.rulesystem.company.infrastructure.web.dto.CreateCompanyRequest;
import com.b4rrhh.rulesystem.company.infrastructure.web.dto.UpdateCompanyRequest;
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
@RequestMapping("/companies")
public class CompanyController {

    private final CreateCompanyUseCase createCompanyUseCase;
    private final ListCompaniesUseCase listCompaniesUseCase;
    private final GetCompanyUseCase getCompanyUseCase;
    private final UpdateCompanyUseCase updateCompanyUseCase;
    private final CompanyResponseAssembler companyResponseAssembler;

    public CompanyController(
            CreateCompanyUseCase createCompanyUseCase,
            ListCompaniesUseCase listCompaniesUseCase,
            GetCompanyUseCase getCompanyUseCase,
            UpdateCompanyUseCase updateCompanyUseCase,
            CompanyResponseAssembler companyResponseAssembler
    ) {
        this.createCompanyUseCase = createCompanyUseCase;
        this.listCompaniesUseCase = listCompaniesUseCase;
        this.getCompanyUseCase = getCompanyUseCase;
        this.updateCompanyUseCase = updateCompanyUseCase;
        this.companyResponseAssembler = companyResponseAssembler;
    }

    @PostMapping
    public ResponseEntity<CompanyResponse> create(@RequestBody CreateCompanyRequest request) {
        CompanyAddressRequest address = request.address();

        Company company = createCompanyUseCase.create(
                new CreateCompanyCommand(
                        request.ruleSystemCode(),
                        request.companyCode(),
                        request.name(),
                        request.description(),
                        request.startDate(),
                        request.legalName(),
                        request.taxIdentifier(),
                        address == null ? null : address.street(),
                        address == null ? null : address.city(),
                        address == null ? null : address.postalCode(),
                        address == null ? null : address.regionCode(),
                        address == null ? null : address.countryCode(),
                        request.cnaeCode()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(companyResponseAssembler.toResponse(company));
    }

    @GetMapping
    public ResponseEntity<List<CompanyListItemResponse>> list() {
        List<CompanyListItemResponse> response = listCompaniesUseCase
                .list(new ListCompaniesQuery(null))
                .stream()
                .map(companyResponseAssembler::toListItemResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ruleSystemCode}/{companyCode}")
    public ResponseEntity<CompanyResponse> get(
            @PathVariable String ruleSystemCode,
            @PathVariable String companyCode
    ) {
        Company company = getCompanyUseCase.get(new GetCompanyQuery(ruleSystemCode, companyCode));
        return ResponseEntity.ok(companyResponseAssembler.toResponse(company));
    }

    @PutMapping("/{ruleSystemCode}/{companyCode}")
    public ResponseEntity<CompanyResponse> update(
            @PathVariable String ruleSystemCode,
            @PathVariable String companyCode,
            @RequestBody UpdateCompanyRequest request
    ) {
        CompanyAddressRequest address = request.address();

        Company company = updateCompanyUseCase.update(
                new UpdateCompanyCommand(
                        ruleSystemCode,
                        companyCode,
                        request.name(),
                        request.description(),
                        request.legalName(),
                        request.taxIdentifier(),
                        address == null ? null : address.street(),
                        address == null ? null : address.city(),
                        address == null ? null : address.postalCode(),
                        address == null ? null : address.regionCode(),
                        address == null ? null : address.countryCode(),
                        request.cnaeCode()
                )
        );

        return ResponseEntity.ok(companyResponseAssembler.toResponse(company));
    }
}
