package com.b4rrhh.rulesystem.workcenter.application.usecase;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.model.LiteralClass;
import com.b4rrhh.rulesystem.domain.model.MaintenanceMode;
import com.b4rrhh.rulesystem.domain.model.RuleEntityType;
import com.b4rrhh.rulesystem.domain.model.RuleSystem;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityTypeRepository;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterCatalogValidator;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterInputNormalizer;
import com.b4rrhh.rulesystem.workcenter.application.view.WorkCenterDetails;
import com.b4rrhh.rulesystem.workcenter.domain.exception.WorkCenterAlreadyExistsException;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterProfile;
import com.b4rrhh.rulesystem.workcenter.domain.port.WorkCenterProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateWorkCenterServiceTest {

    @Mock
    private RuleEntityRepository ruleEntityRepository;
    @Mock
    private RuleSystemRepository ruleSystemRepository;
    @Mock
    private RuleEntityTypeRepository ruleEntityTypeRepository;
    @Mock
    private WorkCenterProfileRepository workCenterProfileRepository;

    private CreateWorkCenterService service;

    @BeforeEach
    void setUp() {
        service = new CreateWorkCenterService(
                ruleEntityRepository,
                ruleSystemRepository,
                ruleEntityTypeRepository,
                workCenterProfileRepository,
                new WorkCenterInputNormalizer(),
                new WorkCenterCatalogValidator(ruleEntityRepository)
        );
    }

    @Test
    void createCreatesWorkCenterAndProfile() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem("ESP")));
        when(ruleEntityTypeRepository.findByCode("WORK_CENTER")).thenReturn(Optional.of(ruleEntityType("WORK_CENTER")));
        when(ruleEntityRepository.findByBusinessKey("ESP", "WORK_CENTER", "MADRID-HQ"))
                .thenReturn(Optional.empty());
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.of(ruleEntity(20L, "ESP", "COMPANY", "ACME", "Acme", LocalDate.of(1900, 1, 1), null, true)));
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "COUNTRY", "ESP", LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.of(ruleEntity(21L, "ESP", "COUNTRY", "ESP", "Spain", LocalDate.of(1900, 1, 1), null, true)));
        when(ruleEntityRepository.save(any(RuleEntity.class)))
                .thenReturn(ruleEntity(10L, "ESP", "WORK_CENTER", "MADRID-HQ", "Madrid HQ", LocalDate.of(2026, 1, 1), null, true));
        when(workCenterProfileRepository.save(any(Long.class), any(WorkCenterProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        WorkCenterDetails result = service.create(new CreateWorkCenterCommand(
                "esp",
                "madrid-hq",
                "Madrid HQ",
                "Headquarters",
                LocalDate.of(2026, 1, 1),
                "acme",
                "Gran Via 1",
                "Madrid",
                "28013",
                "md",
                "esp"
        ));

        ArgumentCaptor<RuleEntity> captor = ArgumentCaptor.forClass(RuleEntity.class);
        verify(ruleEntityRepository).save(captor.capture());
        assertEquals("WORK_CENTER", captor.getValue().getRuleEntityTypeCode());
        assertEquals("MADRID-HQ", captor.getValue().getCode());

        assertEquals("ESP", result.workCenter().ruleSystemCode());
        assertEquals("MADRID-HQ", result.workCenter().workCenterCode());
        assertEquals("ACME", result.profile().getCompanyCode());
        assertEquals("Madrid", result.profile().getAddress().getCity());
    }

    @Test
    void createRejectsDuplicateBusinessKey() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem("ESP")));
        when(ruleEntityTypeRepository.findByCode("WORK_CENTER")).thenReturn(Optional.of(ruleEntityType("WORK_CENTER")));
        when(ruleEntityRepository.findByBusinessKey("ESP", "WORK_CENTER", "MADRID-HQ"))
                .thenReturn(Optional.of(ruleEntity(10L, "ESP", "WORK_CENTER", "MADRID-HQ", "Madrid HQ", LocalDate.of(2026, 1, 1), null, true)));

        assertThrows(WorkCenterAlreadyExistsException.class, () -> service.create(new CreateWorkCenterCommand(
                "ESP",
                "MADRID-HQ",
                "Madrid HQ",
                null,
                LocalDate.of(2026, 1, 1),
                null,
                null,
                null,
                null,
                null,
                null
        )));
    }

    private RuleSystem ruleSystem(String code) {
        return new RuleSystem(1L, code, "Spain", "ESP", true, LocalDateTime.now(), LocalDateTime.now());
    }

    private RuleEntityType ruleEntityType(String code) {
        return new RuleEntityType(1L, code, code,
                LiteralClass.PROPER_NOUN, MaintenanceMode.MAINTAINED, "ORGANIZATION",
                true, LocalDateTime.now(), LocalDateTime.now());
    }

    private RuleEntity ruleEntity(
            Long id,
            String ruleSystemCode,
            String typeCode,
            String code,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            boolean active
    ) {
        return new RuleEntity(id, ruleSystemCode, typeCode, code, name, null, active, startDate, endDate, LocalDateTime.now(), LocalDateTime.now());
    }
}