package com.b4rrhh.employee.identifier.application.service;

import com.b4rrhh.employee.identifier.domain.exception.IdentifierSpanishNationalIdInvalidException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SpanishNationalIdValidator {

    private static final String NATIONAL_ID_TYPE_CODE = "NATIONAL_ID";
    private static final String SPAIN_COUNTRY_CODE = "ESP";
    private static final String LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}[A-Z]$");
    // El NIE de los extranjeros residentes: X, Y o Z, siete cifras y la letra. La letra se
    // calcula igual que la del DNI con la inicial cambiada por 0, 1 o 2 (b4rrhh/backend#141).
    private static final Pattern NIE_PATTERN = Pattern.compile("^[XYZ]\\d{7}[A-Z]$");

    public String normalizeAndValidateIfApplicable(
            String identifierTypeCode,
            String issuingCountryCode,
            String identifierValue
    ) {
        if (identifierValue == null) {
            return null;
        }

        if (!applies(identifierTypeCode, issuingCountryCode)) {
            return identifierValue;
        }

        String normalizedValue = identifierValue.trim().toUpperCase();
        String digits;
        if (DNI_PATTERN.matcher(normalizedValue).matches()) {
            digits = normalizedValue.substring(0, 8);
        } else if (NIE_PATTERN.matcher(normalizedValue).matches()) {
            digits = "XYZ".indexOf(normalizedValue.charAt(0)) + normalizedValue.substring(1, 8);
        } else {
            throw new IdentifierSpanishNationalIdInvalidException();
        }

        int dniNumber = Integer.parseInt(digits);
        char expectedLetter = LETTERS.charAt(dniNumber % 23);
        char providedLetter = normalizedValue.charAt(8);
        if (providedLetter != expectedLetter) {
            throw new IdentifierSpanishNationalIdInvalidException();
        }

        return normalizedValue;
    }

    private boolean applies(String identifierTypeCode, String issuingCountryCode) {
        return NATIONAL_ID_TYPE_CODE.equals(identifierTypeCode)
                && SPAIN_COUNTRY_CODE.equals(issuingCountryCode);
    }
}