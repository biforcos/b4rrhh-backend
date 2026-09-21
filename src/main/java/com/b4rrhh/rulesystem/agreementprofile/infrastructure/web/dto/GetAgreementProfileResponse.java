package com.b4rrhh.rulesystem.agreementprofile.infrastructure.web.dto;

import java.math.BigDecimal;

public record GetAgreementProfileResponse(
        String officialAgreementNumber,
        String displayName,
        String shortName,
        BigDecimal annualHours,
        boolean extraPaymentsProrated,
        boolean active
) {
}
