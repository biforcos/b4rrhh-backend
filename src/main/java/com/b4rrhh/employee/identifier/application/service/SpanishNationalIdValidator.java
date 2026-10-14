package com.b4rrhh.employee.identifier.application.service;

import com.b4rrhh.employee.identifier.domain.exception.IdentifierSpanishNationalIdInvalidException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * El DNI y el NIE expedidos en España: el formato y la letra de control.
 *
 * <p>Dice el motivo en palabras (backend#171): «la letra debería ser Z». La consulta del dueño de un
 * documento lo da al salir del campo y el alta lo da al rechazar, con el mismo texto.</p>
 */
@Component
public class SpanishNationalIdValidator {

    private static final String NATIONAL_ID_TYPE_CODE = "NATIONAL_ID";
    private static final String SPAIN_COUNTRY_CODE = "ESP";
    private static final String LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}[A-Z]$");
    // El NIE de los extranjeros residentes: X, Y o Z, siete cifras y la letra. La letra se
    // calcula igual que la del DNI con la inicial cambiada por 0, 1 o 2 (b4rrhh/backend#141).
    private static final Pattern NIE_PATTERN = Pattern.compile("^[XYZ]\\d{7}[A-Z]$");

    static final String WRONG_FORMAT =
            "un DNI son ocho cifras y una letra, y un NIE una X, Y o Z, siete cifras y una letra";

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
        problemWith(normalizedValue).ifPresent(reason -> {
            throw new IdentifierSpanishNationalIdInvalidException(normalizedValue, reason);
        });
        return normalizedValue;
    }

    /**
     * Por qué el documento no vale, en palabras; vacío si vale o si no es un DNI o un NIE de España,
     * que este validador no juzga.
     */
    public Optional<String> problemWithIfApplicable(
            String identifierTypeCode,
            String issuingCountryCode,
            String identifierValue
    ) {
        if (identifierValue == null || !applies(identifierTypeCode, issuingCountryCode)) {
            return Optional.empty();
        }
        return problemWith(identifierValue.trim().toUpperCase());
    }

    private Optional<String> problemWith(String normalizedValue) {
        String digits;
        if (DNI_PATTERN.matcher(normalizedValue).matches()) {
            digits = normalizedValue.substring(0, 8);
        } else if (NIE_PATTERN.matcher(normalizedValue).matches()) {
            digits = "XYZ".indexOf(normalizedValue.charAt(0)) + normalizedValue.substring(1, 8);
        } else {
            return Optional.of(WRONG_FORMAT);
        }

        char expectedLetter = LETTERS.charAt(Integer.parseInt(digits) % 23);
        if (normalizedValue.charAt(8) != expectedLetter) {
            return Optional.of("la letra debería ser " + expectedLetter);
        }
        return Optional.empty();
    }

    private boolean applies(String identifierTypeCode, String issuingCountryCode) {
        return NATIONAL_ID_TYPE_CODE.equals(identifierTypeCode)
                && SPAIN_COUNTRY_CODE.equals(issuingCountryCode);
    }
}
