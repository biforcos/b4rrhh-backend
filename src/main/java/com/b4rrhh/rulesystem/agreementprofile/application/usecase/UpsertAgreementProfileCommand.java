package com.b4rrhh.rulesystem.agreementprofile.application.usecase;

import java.math.BigDecimal;

public record UpsertAgreementProfileCommand(
        String ruleSystemCode,
        String agreementCode,
        String officialAgreementNumber,
        String displayName,
        String shortName,
        BigDecimal annualHours,
        boolean extraPaymentsProrated,
        boolean active
) {
}
