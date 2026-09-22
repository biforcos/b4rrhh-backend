package com.b4rrhh.rulesystem.companyprofile.infrastructure.web;

import com.b4rrhh.rulesystem.companyprofile.application.usecase.GetCompanyProfileQuery;
import com.b4rrhh.rulesystem.companyprofile.application.usecase.GetCompanyProfileUseCase;
import com.b4rrhh.rulesystem.companyprofile.application.usecase.UpsertCompanyProfileCommand;
import com.b4rrhh.rulesystem.companyprofile.application.usecase.UpsertCompanyProfileUseCase;
import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import com.b4rrhh.rulesystem.companyprofile.infrastructure.web.assembler.CompanyProfileResponseAssembler;
import com.b4rrhh.rulesystem.companyprofile.infrastructure.web.dto.CompanyProfileAddressRequest;
import com.b4rrhh.rulesystem.companyprofile.infrastructure.web.dto.CompanyProfileResponse;
import com.b4rrhh.rulesystem.companyprofile.infrastructure.web.dto.UpsertCompanyProfileRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/companies/{ruleSystemCode}/{companyCode}/profile")
public class CompanyProfileController {

    private final GetCompanyProfileUseCase getCompanyProfileUseCase;
    private final UpsertCompanyProfileUseCase upsertCompanyProfileUseCase;
    private final CompanyProfileResponseAssembler companyProfileResponseAssembler;

    public CompanyProfileController(
            GetCompanyProfileUseCase getCompanyProfileUseCase,
            UpsertCompanyProfileUseCase upsertCompanyProfileUseCase,
            CompanyProfileResponseAssembler companyProfileResponseAssembler
    ) {
        this.getCompanyProfileUseCase = getCompanyProfileUseCase;
        this.upsertCompanyProfileUseCase = upsertCompanyProfileUseCase;
        this.companyProfileResponseAssembler = companyProfileResponseAssembler;
    }

    @GetMapping
    public ResponseEntity<CompanyProfileResponse> get(
            @PathVariable String ruleSystemCode,
            @PathVariable String companyCode
    ) {
        CompanyProfile companyProfile = getCompanyProfileUseCase.get(
                new GetCompanyProfileQuery(ruleSystemCode, companyCode)
        );

        return ResponseEntity.ok(companyProfileResponseAssembler.toResponse(companyCode, companyProfile));
    }

    @PutMapping
    public ResponseEntity<CompanyProfileResponse> upsert(
            @PathVariable String ruleSystemCode,
            @PathVariable String companyCode,
            @RequestBody UpsertCompanyProfileRequest request
    ) {
        CompanyProfileAddressRequest address = request.address();

        CompanyProfile companyProfile = upsertCompanyProfileUseCase.upsert(
                new UpsertCompanyProfileCommand(
                        ruleSystemCode,
                        companyCode,
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

        return ResponseEntity.ok(companyProfileResponseAssembler.toResponse(companyCode, companyProfile));
    }
}