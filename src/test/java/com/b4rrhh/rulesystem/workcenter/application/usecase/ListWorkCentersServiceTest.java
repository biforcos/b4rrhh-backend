package com.b4rrhh.rulesystem.workcenter.application.usecase;

import com.b4rrhh.rulesystem.domain.exception.RequiredExtensionMissingException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterInputNormalizer;
import com.b4rrhh.rulesystem.workcenter.application.view.WorkCenterDetails;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterProfile;
import com.b4rrhh.rulesystem.workcenter.domain.port.WorkCenterProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListWorkCentersServiceTest {

    @Mock private RuleEntityRepository ruleEntityRepository;
    @Mock private WorkCenterProfileRepository workCenterProfileRepository;

    private ListWorkCentersService service;

    @BeforeEach
    void setUp() {
        service = new ListWorkCentersService(ruleEntityRepository, workCenterProfileRepository, new WorkCenterInputNormalizer());
    }

    @Test
    void returnsAllWorkCentersWithProfiles() {
        RuleEntity entity = ruleEntity(10L, "ESP", "MAD-01", "Madrid HQ");
        when(ruleEntityRepository.findByFilters(eq("ESP"), eq("WORK_CENTER"), isNull(), eq(true), any(LocalDate.class)))
                .thenReturn(List.of(entity));
        when(workCenterProfileRepository.findByWorkCenterRuleEntityIds(List.of(10L)))
                .thenReturn(Map.of(10L, new WorkCenterProfile("ES01", null)));

        List<WorkCenterDetails> result = service.list(new ListWorkCentersQuery("ESP"));

        assertEquals(1, result.size());
        assertEquals("MAD-01", result.get(0).workCenter().workCenterCode());
        assertEquals("ES01", result.get(0).profile().getCompanyCode());
    }

    // backend#35: el mismo fallback que en el get, y en la lista todavia mas facil de no
    // ver, porque una ficha vacia entre otras llenas parece una pendiente de rellenar.
    @Test
    void listFailsWhenAWorkCenterHasNoProfile() {
        RuleEntity entity = ruleEntity(10L, "ESP", "MAD-01", "Madrid HQ");
        when(ruleEntityRepository.findByFilters(eq("ESP"), eq("WORK_CENTER"), isNull(), eq(true), any(LocalDate.class)))
                .thenReturn(List.of(entity));
        when(workCenterProfileRepository.findByWorkCenterRuleEntityIds(List.of(10L)))
                .thenReturn(Map.of());

        assertThrows(RequiredExtensionMissingException.class,
                () -> service.list(new ListWorkCentersQuery("ESP")));
    }

    @Test
    void returnsEmptyListWhenNoWorkCentersExist() {
        when(ruleEntityRepository.findByFilters(eq("ESP"), eq("WORK_CENTER"), isNull(), eq(true), any(LocalDate.class)))
                .thenReturn(List.of());
        when(workCenterProfileRepository.findByWorkCenterRuleEntityIds(List.of()))
                .thenReturn(Map.of());

        List<WorkCenterDetails> result = service.list(new ListWorkCentersQuery("ESP"));

        assertEquals(0, result.size());
    }

    @Test
    void passesNullRuleSystemCodeWhenNotProvided() {
        when(ruleEntityRepository.findByFilters(isNull(), eq("WORK_CENTER"), isNull(), eq(true), any(LocalDate.class)))
                .thenReturn(List.of());
        when(workCenterProfileRepository.findByWorkCenterRuleEntityIds(List.of()))
                .thenReturn(Map.of());

        service.list(new ListWorkCentersQuery(null));

        verify(ruleEntityRepository).findByFilters(isNull(), eq("WORK_CENTER"), isNull(), eq(true), any(LocalDate.class));
    }

    private RuleEntity ruleEntity(Long id, String ruleSystemCode, String code, String name) {
        return new RuleEntity(id, ruleSystemCode, "WORK_CENTER", code, name, null, true,
                LocalDate.of(2020, 1, 1), null, LocalDateTime.now(), LocalDateTime.now());
    }
}
