package com.b4rrhh.employee.workcenter.infrastructure.web.assembler;

import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterCompanyLookupPort;
import com.b4rrhh.employee.workcenter.infrastructure.web.dto.WorkCenterResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkCenterResponseAssemblerTest {

    @Mock
    private RuleEntityLabelResolver ruleEntityLabelResolver;
    @Mock
    private WorkCenterCompanyLookupPort workCenterCompanyLookupPort;

    @Test
    void toResponseEnrichesLabelsInTheLanguageOfTheResponse() {
        WorkCenterResponseAssembler assembler = new WorkCenterResponseAssembler(ruleEntityLabelResolver, workCenterCompanyLookupPort);
        WorkCenter workCenter = workCenter(1, "MADRID_HQ", LocalDate.of(2026, 1, 10), null);
        when(ruleEntityLabelResolver.resolveName("ESP", "WORK_CENTER", "MADRID_HQ", "es-ES"))
                .thenReturn(Optional.of("Oficina central"));
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_HQ", LocalDate.of(2026, 1, 10)))
                .thenReturn(Optional.of("COMP"));
        when(ruleEntityLabelResolver.resolveName("ESP", "COMPANY", "COMP", "es-ES"))
                .thenReturn(Optional.of("Compañía principal"));

        WorkCenterResponse response = assembler.toResponse("ESP", workCenter, new ResponseLanguage("es-ES"));

        assertEquals(1, response.workCenterAssignmentNumber());
        assertEquals("MADRID_HQ", response.workCenterCode());
        assertEquals("Oficina central", response.workCenterName());
        assertEquals("COMP", response.companyCode());
        assertEquals("Compañía principal", response.companyName());
    }

    @Test
    void toResponseKeepsCodeAndUsesNullWhenLabelMissing() {
        WorkCenterResponseAssembler assembler = new WorkCenterResponseAssembler(ruleEntityLabelResolver, workCenterCompanyLookupPort);
        WorkCenter workCenter = workCenter(1, "MADRID_HQ", LocalDate.of(2026, 1, 10), null);
        when(ruleEntityLabelResolver.resolveName("ESP", "WORK_CENTER", "MADRID_HQ", null))
                .thenReturn(Optional.empty());
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_HQ", LocalDate.of(2026, 1, 10)))
                .thenReturn(Optional.empty());

        WorkCenterResponse response = assembler.toResponse("ESP", workCenter, ResponseLanguage.base());

        assertEquals("MADRID_HQ", response.workCenterCode());
        assertNull(response.workCenterName());
        assertNull(response.companyCode());
        assertNull(response.companyName());
    }

    private WorkCenter workCenter(int assignmentNumber, String workCenterCode, LocalDate startDate, LocalDate endDate) {
        return new WorkCenter(
                1L,
                10L,
                assignmentNumber,
                workCenterCode,
                startDate,
                endDate,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
