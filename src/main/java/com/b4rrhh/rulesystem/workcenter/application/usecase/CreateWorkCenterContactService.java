package com.b4rrhh.rulesystem.workcenter.application.usecase;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterCatalogValidator;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterInputNormalizer;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterResolver;
import com.b4rrhh.rulesystem.workcenter.domain.exception.WorkCenterContactAlreadyExistsException;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterContact;
import com.b4rrhh.rulesystem.workcenter.domain.port.WorkCenterContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class CreateWorkCenterContactService implements CreateWorkCenterContactUseCase {

    private final WorkCenterResolver workCenterResolver;
    private final WorkCenterInputNormalizer inputNormalizer;
    private final WorkCenterCatalogValidator catalogValidator;
    private final WorkCenterContactRepository workCenterContactRepository;

    public CreateWorkCenterContactService(
            WorkCenterResolver workCenterResolver,
            WorkCenterInputNormalizer inputNormalizer,
            WorkCenterCatalogValidator catalogValidator,
            WorkCenterContactRepository workCenterContactRepository
    ) {
        this.workCenterResolver = workCenterResolver;
        this.inputNormalizer = inputNormalizer;
        this.catalogValidator = catalogValidator;
        this.workCenterContactRepository = workCenterContactRepository;
    }

    @Override
    @Transactional
    public WorkCenterContact create(CreateWorkCenterContactCommand command) {
        String ruleSystemCode = inputNormalizer.normalizeRequiredRuleSystemCode(command.ruleSystemCode());
        String workCenterCode = inputNormalizer.normalizeRequiredWorkCenterCode(command.workCenterCode());
        String contactTypeCode = inputNormalizer.normalizeRequiredContactTypeCode(command.contactTypeCode());
        String contactValue = inputNormalizer.normalizeRequiredContactValue(command.contactValue());

        RuleEntity workCenterEntity = workCenterResolver.resolveApplicableToday(ruleSystemCode, workCenterCode);
        catalogValidator.validateContactTypeCode(ruleSystemCode, contactTypeCode, LocalDate.now());
        Integer contactNumber = workCenterContactRepository.nextContactNumberForWorkCenterRuleEntityId(workCenterEntity.getId());

        workCenterContactRepository
            .findByWorkCenterRuleEntityIdAndContactNumber(workCenterEntity.getId(), contactNumber)
            .ifPresent(existing -> {
                throw new WorkCenterContactAlreadyExistsException(ruleSystemCode, workCenterCode, contactNumber);
            });

        return workCenterContactRepository.save(
                workCenterEntity.getId(),
                new WorkCenterContact(contactNumber, contactTypeCode, contactValue)
        );
    }
}