package com.b4rrhh.rulesystem.agreementprofile.application.usecase;

import com.b4rrhh.rulesystem.agreementprofile.application.port.AgreementCatalogLookupPort;
import com.b4rrhh.rulesystem.agreementprofile.domain.model.AgreementProfile;
import com.b4rrhh.rulesystem.agreementprofile.domain.port.AgreementProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class UpsertAgreementProfileService implements UpsertAgreementProfileUseCase {

    private final AgreementCatalogLookupPort agreementCatalogLookupPort;
    private final AgreementProfileRepository agreementProfileRepository;

    public UpsertAgreementProfileService(
            AgreementCatalogLookupPort agreementCatalogLookupPort,
            AgreementProfileRepository agreementProfileRepository
    ) {
        this.agreementCatalogLookupPort = agreementCatalogLookupPort;
        this.agreementProfileRepository = agreementProfileRepository;
    }

    @Override
    public AgreementProfileResult upsert(UpsertAgreementProfileCommand command) {
        Long agreementRuleEntityId = agreementCatalogLookupPort
                .findAgreementRuleEntityId(command.ruleSystemCode(), command.agreementCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Agreement not found: " + command.ruleSystemCode() + "/" + command.agreementCode()
                ));

        AgreementProfile profile = new AgreementProfile(
                command.officialAgreementNumber(),
                command.displayName(),
                command.shortName(),
                command.annualHours(),
                command.extraPaymentsProrated(),
                command.active()
        );

        AgreementProfile saved = agreementProfileRepository.save(agreementRuleEntityId, profile);
        return toResult(saved);
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
