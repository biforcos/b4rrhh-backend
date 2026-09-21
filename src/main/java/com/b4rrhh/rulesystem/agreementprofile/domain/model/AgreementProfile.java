package com.b4rrhh.rulesystem.agreementprofile.domain.model;

import java.math.BigDecimal;

public class AgreementProfile {

    private static final int OFFICIAL_AGREEMENT_NUMBER_MAX_LENGTH = 50;
    private static final int DISPLAY_NAME_MAX_LENGTH = 200;
    private static final int SHORT_NAME_MAX_LENGTH = 50;
    private static final BigDecimal ANNUAL_HOURS_MIN = new BigDecimal("1");
    private static final BigDecimal ANNUAL_HOURS_MAX = new BigDecimal("9999.99");

    private final String officialAgreementNumber;
    private final String displayName;
    private final String shortName;
    private final BigDecimal annualHours;
    private final boolean extraPaymentsProrated;
    private final boolean active;

    public AgreementProfile(
            String officialAgreementNumber,
            String displayName,
            String shortName,
            BigDecimal annualHours,
            boolean extraPaymentsProrated,
            boolean active
    ) {
        this.officialAgreementNumber = normalizeRequiredText("officialAgreementNumber", officialAgreementNumber, OFFICIAL_AGREEMENT_NUMBER_MAX_LENGTH);
        this.displayName = normalizeRequiredText("displayName", displayName, DISPLAY_NAME_MAX_LENGTH);
        this.shortName = normalizeOptionalText("shortName", shortName, SHORT_NAME_MAX_LENGTH);
        this.annualHours = normalizeRequiredAnnualHours(annualHours);
        this.extraPaymentsProrated = extraPaymentsProrated;
        this.active = active;
    }

    public AgreementProfile update(
            String officialAgreementNumber,
            String displayName,
            String shortName,
            BigDecimal annualHours,
            boolean extraPaymentsProrated,
            boolean active
    ) {
        return new AgreementProfile(
                officialAgreementNumber,
                displayName,
                shortName,
                annualHours,
                extraPaymentsProrated,
                active
        );
    }

    public String getOfficialAgreementNumber() {
        return officialAgreementNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortName() {
        return shortName;
    }

    public BigDecimal getAnnualHours() {
        return annualHours;
    }

    /**
     * Si el convenio prorratea las pagas extras por defecto (backend#117).
     *
     * <p>No es lo que decide el recibo. Lo que lo decide es la vertical del empleado, y este
     * testigo es lo que la contratacion copia cuando nadie dice otra cosa. Es una copia y no un
     * enlace: si el convenio cambia el ano que viene, los que ya estan no cambian solos.
     */
    public boolean isExtraPaymentsProrated() {
        return extraPaymentsProrated;
    }

    public boolean isActive() {
        return active;
    }

    private String normalizeRequiredText(String fieldName, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " exceeds maximum length " + maxLength);
        }
        return trimmed;
    }

    private String normalizeOptionalText(String fieldName, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " exceeds maximum length " + maxLength);
        }
        return trimmed;
    }

    private BigDecimal normalizeRequiredAnnualHours(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("annualHours is required");
        }
        if (value.compareTo(ANNUAL_HOURS_MIN) < 0 || value.compareTo(ANNUAL_HOURS_MAX) > 0) {
            throw new IllegalArgumentException("annualHours must be between " + ANNUAL_HOURS_MIN + " and " + ANNUAL_HOURS_MAX);
        }
        return value;
    }
}
