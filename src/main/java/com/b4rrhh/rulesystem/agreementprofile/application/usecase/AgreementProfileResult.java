package com.b4rrhh.rulesystem.agreementprofile.application.usecase;

import java.math.BigDecimal;

public record AgreementProfileResult(
        String officialAgreementNumber,
        String displayName,
        String shortName,
        BigDecimal annualHours,
        boolean extraPaymentsProrated,
        boolean active
) {
}
