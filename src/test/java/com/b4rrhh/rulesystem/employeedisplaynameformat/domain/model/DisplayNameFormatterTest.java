package com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayNameFormatterTest {

    /**
     * Los seis formatos, con la salida que la pantalla de configuración promete para cada
     * uno (backend#42). Recorre el enum entero: un formato nuevo sin su fila aquí hace
     * fallar el test, en vez de quedarse sin probar.
     */
    private static final Map<DisplayNameFormatCode, String> EXPECTED_FOR_JUAN_ANTONIO_BIFORCOS_AMOR = Map.of(
            DisplayNameFormatCode.FULL_TITLE_CASE,      "Juan Antonio Biforcos Amor",
            DisplayNameFormatCode.FULL_UPPER,           "JUAN ANTONIO BIFORCOS AMOR",
            DisplayNameFormatCode.SURNAME_FIRST_UPPER,  "BIFORCOS AMOR, JUAN ANTONIO",
            DisplayNameFormatCode.SHORT_TITLE,          "Juan Antonio Biforcos",
            DisplayNameFormatCode.SHORT_UPPER,          "JUAN ANTONIO BIFORCOS",
            DisplayNameFormatCode.SURNAME_ABBREV_UPPER, "BIFORCOS AMOR, J.A."
    );

    @ParameterizedTest
    @EnumSource(DisplayNameFormatCode.class)
    void everyFormatProducesWhatItPromises(DisplayNameFormatCode formatCode) {
        assertThat(EXPECTED_FOR_JUAN_ANTONIO_BIFORCOS_AMOR)
                .as("formato sin salida esperada en el test")
                .containsKey(formatCode);

        String result = DisplayNameFormatter.format("juan antonio", "biforcos", "amor", formatCode);

        assertThat(result).isEqualTo(EXPECTED_FOR_JUAN_ANTONIO_BIFORCOS_AMOR.get(formatCode));
    }

    @Test
    void fullTitleCase_titleCasesAllWords() {
        String result = DisplayNameFormatter.format("juan antonio", "biforcos", "amor", DisplayNameFormatCode.FULL_TITLE_CASE);
        assertThat(result).isEqualTo("Juan Antonio Biforcos Amor");
    }

    @Test
    void fullUpper_uppercasesEverything() {
        String result = DisplayNameFormatter.format("Juan Antonio", "Biforcos", "Amor", DisplayNameFormatCode.FULL_UPPER);
        assertThat(result).isEqualTo("JUAN ANTONIO BIFORCOS AMOR");
    }

    @Test
    void surnameFirstUpper_surnamesCommaFirstName() {
        String result = DisplayNameFormatter.format("Juan Antonio", "Biforcos", "Amor", DisplayNameFormatCode.SURNAME_FIRST_UPPER);
        assertThat(result).isEqualTo("BIFORCOS AMOR, JUAN ANTONIO");
    }

    @Test
    void shortTitle_firstNamePlusFirstSurnameOnly() {
        String result = DisplayNameFormatter.format("Juan Antonio", "Biforcos", "Amor", DisplayNameFormatCode.SHORT_TITLE);
        assertThat(result).isEqualTo("Juan Antonio Biforcos");
    }

    @Test
    void shortUpper_firstNamePlusFirstSurnameUppercase() {
        String result = DisplayNameFormatter.format("Juan Antonio", "Biforcos", "Amor", DisplayNameFormatCode.SHORT_UPPER);
        assertThat(result).isEqualTo("JUAN ANTONIO BIFORCOS");
    }

    @Test
    void surnameAbbrevUpper_surnamesPlusInitials() {
        String result = DisplayNameFormatter.format("Juan Antonio", "Biforcos", "Amor", DisplayNameFormatCode.SURNAME_ABBREV_UPPER);
        assertThat(result).isEqualTo("BIFORCOS AMOR, J.A.");
    }

    @Test
    void nullLastName2_omitsIt() {
        String result = DisplayNameFormatter.format("Juan", "Garcia", null, DisplayNameFormatCode.FULL_TITLE_CASE);
        assertThat(result).isEqualTo("Juan Garcia");
    }

    @Test
    void surnameFirstUpper_nullLastName2_noTrailingSpace() {
        String result = DisplayNameFormatter.format("Juan", "Garcia", null, DisplayNameFormatCode.SURNAME_FIRST_UPPER);
        assertThat(result).isEqualTo("GARCIA, JUAN");
    }

    @Test
    void singleWordFirstName_surnameAbbrevUpper_singleInitial() {
        String result = DisplayNameFormatter.format("Juan", "Biforcos", "Amor", DisplayNameFormatCode.SURNAME_ABBREV_UPPER);
        assertThat(result).isEqualTo("BIFORCOS AMOR, J.");
    }

    @Test
    void blankLastName2_treatedAsAbsent() {
        String result = DisplayNameFormatter.format("Juan", "Garcia", "   ", DisplayNameFormatCode.FULL_TITLE_CASE);
        assertThat(result).isEqualTo("Juan Garcia");
    }

    @Test
    void surnameFirstUpper_mixedCaseInput_uppercasesCorrectly() {
        String result = DisplayNameFormatter.format("juaN aNtoniO", "biForCoS", "aMoR", DisplayNameFormatCode.SURNAME_FIRST_UPPER);
        assertThat(result).isEqualTo("BIFORCOS AMOR, JUAN ANTONIO");
    }

    @Test
    void surnameAbbrevUpper_nullFirstName_noLeadingComma() {
        String result = DisplayNameFormatter.format(null, "Biforcos", "Amor", DisplayNameFormatCode.SURNAME_ABBREV_UPPER);
        assertThat(result).isEqualTo("BIFORCOS AMOR");
    }

    @Test
    void fullTitleCase_multipleInternalSpaces_collapsed() {
        String result = DisplayNameFormatter.format("Juan   Antonio", "Biforcos", "Amor", DisplayNameFormatCode.FULL_TITLE_CASE);
        assertThat(result).isEqualTo("Juan Antonio Biforcos Amor");
    }
}
