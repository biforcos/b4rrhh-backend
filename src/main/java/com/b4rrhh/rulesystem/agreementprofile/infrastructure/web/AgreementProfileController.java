package com.b4rrhh.rulesystem.agreementprofile.infrastructure.web;

import com.b4rrhh.rulesystem.agreementprofile.application.usecase.GetAgreementProfileQuery;
import com.b4rrhh.rulesystem.agreementprofile.application.usecase.GetAgreementProfileUseCase;
import com.b4rrhh.rulesystem.agreementprofile.application.usecase.UpsertAgreementProfileCommand;
import com.b4rrhh.rulesystem.agreementprofile.application.usecase.UpsertAgreementProfileUseCase;
import com.b4rrhh.rulesystem.agreementprofile.infrastructure.web.dto.GetAgreementProfileResponse;
import com.b4rrhh.rulesystem.agreementprofile.infrastructure.web.dto.UpsertAgreementProfileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agreements/{ruleSystemCode}/{agreementCode}/profile")
public class AgreementProfileController {

    private final GetAgreementProfileUseCase getAgreementProfileUseCase;
    private final UpsertAgreementProfileUseCase upsertAgreementProfileUseCase;

    public AgreementProfileController(
            GetAgreementProfileUseCase getAgreementProfileUseCase,
            UpsertAgreementProfileUseCase upsertAgreementProfileUseCase
    ) {
        this.getAgreementProfileUseCase = getAgreementProfileUseCase;
        this.upsertAgreementProfileUseCase = upsertAgreementProfileUseCase;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(
            @PathVariable String ruleSystemCode,
            @PathVariable String agreementCode
    ) {
        return getAgreementProfileUseCase.get(new GetAgreementProfileQuery(ruleSystemCode, agreementCode))
                .map(result -> ResponseEntity.ok(new GetAgreementProfileResponse(
                        result.officialAgreementNumber(),
                        result.displayName(),
                        result.shortName(),
                        result.annualHours(),
                        result.extraPaymentsProrated(),
                        result.active()
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<?> upsertProfile(
            @PathVariable String ruleSystemCode,
            @PathVariable String agreementCode,
            @RequestBody UpsertAgreementProfileRequest request
    ) {
        try {
            var result = upsertAgreementProfileUseCase.upsert(new UpsertAgreementProfileCommand(
                    ruleSystemCode,
                    agreementCode,
                    request.officialAgreementNumber(),
                    request.displayName(),
                    request.shortName(),
                    request.annualHours(),
                    request.extraPaymentsProrated(),
                    request.active()
            ));

            return ResponseEntity.ok(new GetAgreementProfileResponse(
                    result.officialAgreementNumber(),
                    result.displayName(),
                    result.shortName(),
                    result.annualHours(),
                    result.extraPaymentsProrated(),
                    result.active()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
