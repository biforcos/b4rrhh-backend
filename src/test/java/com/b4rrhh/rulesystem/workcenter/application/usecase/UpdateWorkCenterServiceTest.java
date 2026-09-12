package com.b4rrhh.rulesystem.workcenter.application.usecase;

import com.b4rrhh.rulesystem.domain.exception.RequiredExtensionMissingException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterCatalogValidator;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterInputNormalizer;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterResolver;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateWorkCenterServiceTest {

    @Mock private RuleEntityRepository ruleEntityRepository;
    @Mock private WorkCenterProfileRepository workCenterProfileRepository;

    private UpdateWorkCenterService service;

    @BeforeEach
    void setUp() {
        service = new UpdateWorkCenterService(
                new WorkCenterResolver(ruleEntityRepository),
                new WorkCenterInputNormalizer(),
                new WorkCenterCatalogValidator(ruleEntityRepository),
                ruleEntityRepository,
                workCenterProfileRepository
        );
    }

    @Test
    void updatesWorkCenterNameAndProfile() {
        RuleEntity entity = ruleEntity(10L, "ESP", "MAD-01", "Old Name");
        when(ruleEntityRepository.findApplicableByBusinessKey(eq("ESP"), eq("WORK_CENTER"), eq("MAD-01"), any(LocalDate.class)))
                .thenReturn(Optional.of(entity));
        when(ruleEntityRepository.findApplicableByBusinessKey(eq("ESP"), eq("COMPANY"), eq("ES01"), any(LocalDate.class)))
                .thenReturn(Optional.of(ruleEntity(20L, "ESP", "ES01", "Company")));
        when(ruleEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(workCenterProfileRepository.findByWorkCenterRuleEntityId(10L))
                .thenReturn(Optional.of(new WorkCenterProfile(null, null)));
        when(workCenterProfileRepository.save(anyLong(), any())).thenAnswer(inv -> inv.getArgument(1));

        WorkCenterDetails result = service.update(new UpdateWorkCenterCommand(
                "ESP", "MAD-01", "New Name", null, "ES01", null, null, null, null, null));

        assertEquals("New Name", result.workCenter().name());
        assertEquals("ES01", result.profile().getCompanyCode());
    }

    @Test
    void updatesWithNullCompanyCode() {
        RuleEntity entity = ruleEntity(10L, "ESP", "MAD-01", "Madrid HQ");
        when(ruleEntityRepository.findApplicableByBusinessKey(eq("ESP"), eq("WORK_CENTER"), eq("MAD-01"), any(LocalDate.class)))
                .thenReturn(Optional.of(entity));
        when(ruleEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(workCenterProfileRepository.findByWorkCenterRuleEntityId(10L))
                .thenReturn(Optional.of(new WorkCenterProfile("ES01", null)));
        when(workCenterProfileRepository.save(anyLong(), any())).thenAnswer(inv -> inv.getArgument(1));

        WorkCenterDetails result = service.update(new UpdateWorkCenterCommand(
                "ESP", "MAD-01", "Madrid HQ", null, null, null, null, null, null, null));

        assertNull(result.profile().getCompanyCode());
    }

    // backend#35: antes el update de un centro sin perfil le creaba uno fabricado al vuelo.
    // Ahora falla, y falla sin escribir el perfil.
    @Test
    void updateFailsWhenProfileMissing() {
        RuleEntity entity = ruleEntity(10L, "ESP", "MAD-01", "Madrid HQ");
        when(ruleEntityRepository.findApplicableByBusinessKey(eq("ESP"), eq("WORK_CENTER"), eq("MAD-01"), any(LocalDate.class)))
                .thenReturn(Optional.of(entity));
        when(ruleEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(workCenterProfileRepository.findByWorkCenterRuleEntityId(10L)).thenReturn(Optional.empty());

        assertThrows(RequiredExtensionMissingException.class, () -> service.update(new UpdateWorkCenterCommand(
                "ESP", "MAD-01", "Madrid HQ", null, null, null, null, null, null, null)));

        verify(workCenterProfileRepository, never()).save(anyLong(), any());
    }

    private RuleEntity ruleEntity(Long id, String ruleSystemCode, String code, String name) {
        return new RuleEntity(id, ruleSystemCode, "WORK_CENTER", code, name, null, true,
                LocalDate.of(2020, 1, 1), null, LocalDateTime.now(), LocalDateTime.now());
    }
}
