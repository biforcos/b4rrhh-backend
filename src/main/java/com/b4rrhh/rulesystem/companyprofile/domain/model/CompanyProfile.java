package com.b4rrhh.rulesystem.companyprofile.domain.model;

public class CompanyProfile {

    private static final int LEGAL_NAME_MAX_LENGTH = 200;
    private static final int TAX_IDENTIFIER_MAX_LENGTH = 50;
    private static final int STREET_MAX_LENGTH = 300;
    private static final int CITY_MAX_LENGTH = 120;
    private static final int POSTAL_CODE_MAX_LENGTH = 20;
    private static final int REGION_CODE_MAX_LENGTH = 30;
    private static final int CNAE_CODE_MAX_LENGTH = 10;

    private final String legalName;
    private final String taxIdentifier;
    private final String street;
    private final String city;
    private final String postalCode;
    private final String regionCode;
    private final String countryCode;
    /**
     * La actividad economica de la empresa, en CNAE ({@code backend#122}).
     *
     * <p>Se llamaba {@code epigrafeAtCode} y el nombre estaba mal: el «epigrafe» es la tarifa de
     * primas anterior a 2007 y hoy no significa nada. Lo que la Seguridad Social usa desde
     * entonces para cotizar por accidentes de trabajo es el codigo CNAE de la actividad, y ese es
     * el dato que se guarda aqui.
     *
     * <p>Es una propiedad de la EMPRESA y no de la nomina: de el sale el tipo de la cuota de
     * accidentes de trabajo y enfermedad profesional, buscando en la tarifa de primas la entrada
     * mas especifica que lo cubra.
     */
    private final String cnaeCode;

    public CompanyProfile(
            String legalName,
            String taxIdentifier,
            String street,
            String city,
            String postalCode,
            String regionCode,
            String countryCode,
            String cnaeCode
    ) {
        this.legalName = normalizeRequiredText("legalName", legalName, LEGAL_NAME_MAX_LENGTH);
        this.taxIdentifier = normalizeOptionalText("taxIdentifier", taxIdentifier, TAX_IDENTIFIER_MAX_LENGTH);
        this.street = normalizeOptionalText("street", street, STREET_MAX_LENGTH);
        this.city = normalizeOptionalText("city", city, CITY_MAX_LENGTH);
        this.postalCode = normalizeOptionalText("postalCode", postalCode, POSTAL_CODE_MAX_LENGTH);
        this.regionCode = normalizeOptionalCode("regionCode", regionCode, REGION_CODE_MAX_LENGTH);
        this.countryCode = normalizeOptionalCode(countryCode);
        this.cnaeCode = normalizeOptionalText("cnaeCode", cnaeCode, CNAE_CODE_MAX_LENGTH);
    }

    public CompanyProfile update(
            String legalName,
            String taxIdentifier,
            String street,
            String city,
            String postalCode,
            String regionCode,
            String countryCode,
            String cnaeCode
    ) {
        return new CompanyProfile(
                legalName,
                taxIdentifier,
                street,
                city,
                postalCode,
                regionCode,
                countryCode,
                cnaeCode
        );
    }

    public String getLegalName() {
        return legalName;
    }

    public String getTaxIdentifier() {
        return taxIdentifier;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCnaeCode() {
        return cnaeCode;
    }

    private String normalizeRequiredText(String fieldName, String value, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        String normalized = value.trim();
        validateLength(fieldName, normalized, maxLength);
        return normalized;
    }

    private String normalizeOptionalText(String fieldName, String value, int maxLength) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        validateLength(fieldName, normalized, maxLength);
        return normalized;
    }

    private String normalizeOptionalCode(String value) {
        String normalized = normalizeOptionalText("countryCode", value, 3);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String normalizeOptionalCode(String fieldName, String value, int maxLength) {
        String normalized = normalizeOptionalText(fieldName, value, maxLength);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private void validateLength(String fieldName, String value, int maxLength) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " exceeds max length " + maxLength);
        }
    }
}