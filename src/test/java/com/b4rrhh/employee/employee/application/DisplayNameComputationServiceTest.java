package com.b4rrhh.employee.employee.application;

import com.b4rrhh.employee.employee.application.port.DisplayNameFormatLookupPort;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.DisplayNameFormatCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class DisplayNameComputationServiceTest {

    @Mock DisplayNameFormatLookupPort formatLookupPort;
    @InjectMocks DisplayNameComputationService service;

    /**
     * El sustituto es un nombre completo distinto del de pila (backend#42): «Bifor» no sale
     * de «Juan Antonio», así que si el resultado lo contiene es porque ganó el sustituto, y
     * si contiene un apellido es porque alguien lo compuso con el formato, que es justo lo
     * que no debe pasar.
     */
    @Test
    void substitute_isShownAloneAndTheFormatIsNotEvenLookedUp() {
        String result = service.compute("RS1", "Juan Antonio", "Biforcos", "Amor", "Bifor");

        assertThat(result).isEqualTo("Bifor");
        verifyNoInteractions(formatLookupPort);
    }

    @Test
    void withoutSubstitute_theRuleSystemFormatDecides() {
        when(formatLookupPort.findFormatCodeForRuleSystem("RS1"))
                .thenReturn(Optional.of(DisplayNameFormatCode.SURNAME_FIRST_UPPER));

        String result = service.compute("RS1", "Juan Antonio", "Biforcos", "Amor", null);

        assertThat(result).isEqualTo("BIFORCOS AMOR, JUAN ANTONIO");
    }

    /**
     * Sin formato no se imita ninguno: lo escrito sale tal cual, sin capitalizar ni
     * reordenar. Con la entrada en minúsculas se ve que no hay formato detrás.
     */
    @Test
    void withoutFormat_showsTheNameAsEnteredWithoutImitatingAnyFormat() {
        when(formatLookupPort.findFormatCodeForRuleSystem("RS1"))
                .thenReturn(Optional.empty());

        String result = service.compute("RS1", "juan antonio", "biforcos", "amor", null);

        assertThat(result).isEqualTo("juan antonio biforcos amor");
    }

    @Test
    void withoutFormat_warnsOncePerRuleSystemNamingTheFix(CapturedOutput output) {
        when(formatLookupPort.findFormatCodeForRuleSystem("FRA")).thenReturn(Optional.empty());
        when(formatLookupPort.findFormatCodeForRuleSystem("PRT")).thenReturn(Optional.empty());

        service.compute("FRA", "Jean", "Dupont", null, null);
        service.compute("FRA", "Marie", "Curie", null, null);
        service.compute("PRT", "Joao", "Silva", null, null);

        assertThat(output.getOut())
                .containsOnlyOnce("Rule system FRA has no employee display name format configured")
                .containsOnlyOnce("Rule system PRT has no employee display name format configured")
                .contains("PUT /rule-systems/FRA/employee-display-name-format");
    }

    @Test
    void fallback_skipsNullLastName2() {
        when(formatLookupPort.findFormatCodeForRuleSystem("RS1"))
                .thenReturn(Optional.empty());

        String result = service.compute("RS1", "Juan", "Garcia", null, null);

        assertThat(result).isEqualTo("Juan Garcia");
    }

    @Test
    void blankPreferredName_treatedAsNull() {
        when(formatLookupPort.findFormatCodeForRuleSystem("RS1"))
                .thenReturn(Optional.of(DisplayNameFormatCode.FULL_UPPER));

        String result = service.compute("RS1", "Juan", "Garcia", null, "   ");

        assertThat(result).isEqualTo("JUAN GARCIA");
    }
}
