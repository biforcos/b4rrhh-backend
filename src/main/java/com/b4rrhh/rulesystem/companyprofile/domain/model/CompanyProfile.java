package com.b4rrhh.rulesystem.companyprofile.domain.model;

import com.b4rrhh.geo.address.domain.model.PostalAddress;

public class CompanyProfile {

    private static final int LEGAL_NAME_MAX_LENGTH = 200;
    private static final int TAX_IDENTIFIER_MAX_LENGTH = 50;
    private static final int CNAE_CODE_MAX_LENGTH = 10;

    /**
     * La unica clasificacion de actividad que este sistema sabe guardar hoy ({@code backend#122}).
     *
     * <p>Es la que entiende la tarifa de primas vigente (RD 10/2025). No se pide por la API ni se
     * edita: <b>se deriva</b> de que haya codigo, porque mientras solo haya una admisible dejar
     * elegir seria dejar equivocarse. Admitir una segunda no es anadir un valor, es traer la tabla
     * de correspondencias oficial —la DT 45.a de la LGSS— y decidir quien convierte y cuando.
     */
    public static final String CNAE_CLASSIFICATION = "CNAE-2025";

    private final String legalName;
    private final String taxIdentifier;
    /** La dirección, la misma estructura que la del empleado y la del centro; {@code null} si no tiene (backend#155). */
    private final PostalAddress address;
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
    /**
     * En que clasificacion esta escrito {@link #cnaeCode}, o {@code null} si no hay codigo.
     *
     * <p>Un codigo de cuatro digitos vale en CNAE-2009 y en CNAE-2025 y quiere decir cosas
     * distintas: el {@code 4719} que la {@code V150} sembro era CNAE-2009 y se resolvia contra una
     * tarifa de CNAE-2025 sin que nada lo notara.
     */
    private final String cnaeClassification;

    public CompanyProfile(
            String legalName,
            String taxIdentifier,
            PostalAddress address,
            String cnaeCode
    ) {
        this.legalName = normalizeRequiredText("legalName", legalName, LEGAL_NAME_MAX_LENGTH);
        this.taxIdentifier = normalizeOptionalText("taxIdentifier", taxIdentifier, TAX_IDENTIFIER_MAX_LENGTH);
        this.address = address;
        this.cnaeCode = normalizeOptionalText("cnaeCode", cnaeCode, CNAE_CODE_MAX_LENGTH);
        this.cnaeClassification = this.cnaeCode == null ? null : CNAE_CLASSIFICATION;
    }

    public CompanyProfile update(
            String legalName,
            String taxIdentifier,
            PostalAddress address,
            String cnaeCode
    ) {
        return new CompanyProfile(
                legalName,
                taxIdentifier,
                address,
                cnaeCode
        );
    }

    public String getCnaeClassification() {
        return cnaeClassification;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getTaxIdentifier() {
        return taxIdentifier;
    }

    public PostalAddress getAddress() {
        return address;
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

    private void validateLength(String fieldName, String value, int maxLength) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " exceeds max length " + maxLength);
        }
    }
}