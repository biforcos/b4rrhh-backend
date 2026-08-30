package com.b4rrhh.employee.presence.infrastructure.web.assembler;

import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.presence.infrastructure.web.dto.PresenceResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceResponseAssemblerTest {

    private static final String COMPANY = "COMPANY";
    private static final String ENTRY_REASON = "EMPLOYEE_PRESENCE_ENTRY_REASON";
    private static final String EXIT_REASON = "EMPLOYEE_PRESENCE_EXIT_REASON";

    @Mock
    private RuleEntityLabelResolver ruleEntityLabelResolver;

    @Test
    void toResponseEnrichesLabelsInTheLanguageOfTheResponse() {
        PresenceResponseAssembler assembler = new PresenceResponseAssembler(ruleEntityLabelResolver);
        Presence presence = presence(1, "AC01", "ENT01", "EXT01");
        when(ruleEntityLabelResolver.resolveName("ESP", COMPANY, "AC01", "es-ES"))
                .thenReturn(Optional.of("Empresa Activa"));
        when(ruleEntityLabelResolver.resolveName("ESP", ENTRY_REASON, "ENT01", "es-ES"))
                .thenReturn(Optional.of("Alta inicial"));
        when(ruleEntityLabelResolver.resolveName("ESP", EXIT_REASON, "EXT01", "es-ES"))
                .thenReturn(Optional.of("Baja voluntaria"));

        PresenceResponse response = assembler.toResponse("ESP", presence, new ResponseLanguage("es-ES"));

        assertEquals(1, response.presenceNumber());
        assertEquals("AC01", response.companyCode());
        assertEquals("Empresa Activa", response.companyName());
        assertEquals("Alta inicial", response.entryReasonName());
        assertEquals("Baja voluntaria", response.exitReasonName());
    }

    @Test
    void toResponseAsksForTheBaseLiteralWhenTheResponseHasNoLanguage() {
        PresenceResponseAssembler assembler = new PresenceResponseAssembler(ruleEntityLabelResolver);
        Presence presence = presence(1, "AC01", "ENT01", null);
        lenient().when(ruleEntityLabelResolver.resolveName("ESP", ENTRY_REASON, "ENT01", null))
                .thenReturn(Optional.of("Hiring"));

        PresenceResponse response = assembler.toResponse("ESP", presence, ResponseLanguage.base());

        assertEquals("Hiring", response.entryReasonName());
    }

    @Test
    void toResponseKeepsTheCodeAndUsesNullWhenTheLabelIsMissing() {
        PresenceResponseAssembler assembler = new PresenceResponseAssembler(ruleEntityLabelResolver);
        Presence presence = presence(1, "AC01", "ENT01", "EXT01");

        PresenceResponse response = assembler.toResponse("ESP", presence, ResponseLanguage.base());

        assertEquals("AC01", response.companyCode());
        assertNull(response.companyName());
        assertNull(response.entryReasonName());
        assertNull(response.exitReasonName());
    }

    private Presence presence(int presenceNumber, String companyCode, String entryReasonCode, String exitReasonCode) {
        return new Presence(
                20L,
                10L,
                presenceNumber,
                companyCode,
                entryReasonCode,
                exitReasonCode,
                LocalDate.of(2026, 1, 10),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
