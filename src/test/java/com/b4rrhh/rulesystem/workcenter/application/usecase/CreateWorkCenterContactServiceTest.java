package com.b4rrhh.rulesystem.workcenter.application.usecase;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterCatalogValidator;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterInputNormalizer;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterResolver;
import com.b4rrhh.rulesystem.workcenter.domain.exception.WorkCenterContactAlreadyExistsException;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterContact;
import com.b4rrhh.rulesystem.workcenter.domain.port.WorkCenterContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class CreateWorkCenterContactServiceTest {

    @Mock
    private RuleEntityRepository ruleEntityRepository;
    @Mock
    private WorkCenterContactRepository workCenterContactRepository;

    private CreateWorkCenterContactService service;

    @BeforeEach
    void setUp() {
        service = new CreateWorkCenterContactService(
                new WorkCenterResolver(ruleEntityRepository),
                new WorkCenterInputNormalizer(),
                new WorkCenterCatalogValidator(ruleEntityRepository),
                workCenterContactRepository
        );
    }

    @Test
    void createCreatesContactUsingContactNumberAsBusinessKey() {
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "WORK_CENTER", "MADRID-HQ", LocalDate.now()))
                .thenReturn(Optional.of(ruleEntity(10L, "WORK_CENTER", "MADRID-HQ")));
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "CONTACT_TYPE", "EMAIL", LocalDate.now()))
                .thenReturn(Optional.of(ruleEntity(20L, "CONTACT_TYPE", "EMAIL")));
        when(workCenterContactRepository.nextContactNumberForWorkCenterRuleEntityId(10L)).thenReturn(1);
        when(workCenterContactRepository.findByWorkCenterRuleEntityIdAndContactNumber(10L, 1))
                .thenReturn(Optional.empty());
        when(workCenterContactRepository.save(any(Long.class), any(WorkCenterContact.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        WorkCenterContact result = service.create(new CreateWorkCenterContactCommand(
                "esp",
                "madrid-hq",
                "email",
                "hq@example.com"
        ));

        assertEquals(1, result.getContactNumber());
        assertEquals("EMAIL", result.getContactTypeCode());
        verify(workCenterContactRepository).save(any(Long.class), any(WorkCenterContact.class));
    }

    @Test
    void createRejectsDuplicateContactNumberWithinWorkCenter() {
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "WORK_CENTER", "MADRID-HQ", LocalDate.now()))
                .thenReturn(Optional.of(ruleEntity(10L, "WORK_CENTER", "MADRID-HQ")));
        when(ruleEntityRepository.findApplicableByBusinessKey("ESP", "CONTACT_TYPE", "EMAIL", LocalDate.now()))
                .thenReturn(Optional.of(ruleEntity(20L, "CONTACT_TYPE", "EMAIL")));
        when(workCenterContactRepository.nextContactNumberForWorkCenterRuleEntityId(10L)).thenReturn(1);
        when(workCenterContactRepository.findByWorkCenterRuleEntityIdAndContactNumber(10L, 1))
                .thenReturn(Optional.of(new WorkCenterContact(1, "EMAIL", "existing@example.com")));

        assertThrows(WorkCenterContactAlreadyExistsException.class, () -> service.create(new CreateWorkCenterContactCommand(
                "ESP",
                "MADRID-HQ",
                "EMAIL",
                "hq@example.com"
        )));
    }

    private RuleEntity ruleEntity(Long id, String typeCode, String code) {
        return new RuleEntity(
                id,
                "ESP",
                typeCode,
                code,
                code,
                null,
                true,
                LocalDate.of(1900, 1, 1),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}