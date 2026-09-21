package com.b4rrhh.rulesystem.agreementprofile.application.usecase;

import com.b4rrhh.rulesystem.agreementprofile.application.port.AgreementCatalogLookupPort;
import com.b4rrhh.rulesystem.agreementprofile.domain.model.AgreementProfile;
import com.b4rrhh.rulesystem.agreementprofile.domain.port.AgreementProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetAgreementProfileService implements GetAgreementProfileUseCase {

    private final AgreementCatalogLookupPort agreementCatalogLookupPort;
    private final AgreementProfileRepository agreementProfileRepository;

    public GetAgreementProfileService(
            AgreementCatalogLookupPort agreementCatalogLookupPort,
            AgreementProfileRepository agreementProfileRepository
    ) {
        this.agreementCatalogLookupPort = agreementCatalogLookupPort;
        this.agreementProfileRepository = agreementProfileRepository;
    }

    @Override
    public Optional<AgreementProfileResult> get(GetAgreementProfileQuery query) {
        return agreementCatalogLookupPort.findAgreementRuleEntityId(query.ruleSystemCode(), query.agreementCode())
                .flatMap(agreementProfileRepository::findByAgreementRuleEntityId)
                .map(this::toResult);
    }

    private AgreementProfileResult toResult(AgreementProfile profile) {
        return new AgreementProfileResult(
                profile.getOfficialAgreementNumber(),
                profile.getDisplayName(),
                profile.getShortName(),
                profile.getAnnualHours(),
                profile.isExtraPaymentsProrated(),
                profile.isActive()
        );
    }
}
