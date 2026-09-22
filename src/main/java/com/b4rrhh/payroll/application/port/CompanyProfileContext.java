package com.b4rrhh.payroll.application.port;

public record CompanyProfileContext(
        String legalName,
        String taxIdentifier,
        String street,
        String city,
        String postalCode,
        /** La actividad economica de la empresa, en CNAE. Puede ser nula ({@code backend#122}). */
        String cnaeCode
) {}
