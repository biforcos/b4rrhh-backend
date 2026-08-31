package com.b4rrhh.rulesystem.workcenter.application.usecase;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterInputNormalizer;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterResolver;
import com.b4rrhh.rulesystem.workcenter.domain.exception.WorkCenterContactNotFoundException;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterContact;
import com.b4rrhh.rulesystem.workcenter.domain.port.WorkCenterContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetWorkCenterContactService implements GetWorkCenterContactUseCase {

    private final WorkCenterResolver workCenterResolver;
    private final WorkCenterInputNormalizer inputNormalizer;
    private final WorkCenterContactRepository workCenterContactRepository;

    public GetWorkCenterContactService(
            WorkCenterResolver workCenterResolver,
            WorkCenterInputNormalizer inputNormalizer,
            WorkCenterContactRepository workCenterContactRepository
    ) {
        this.workCenterResolver = workCenterResolver;
        this.inputNormalizer = inputNormalizer;
        this.workCenterContactRepository = workCenterContactRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkCenterContact get(GetWorkCenterContactQuery query) {
        String ruleSystemCode = inputNormalizer.normalizeRequiredRuleSystemCode(query.ruleSystemCode());
        String workCenterCode = inputNormalizer.normalizeRequiredWorkCenterCode(query.workCenterCode());
        Integer contactNumber = inputNormalizer.normalizeRequiredContactNumber(query.contactNumber());

        RuleEntity workCenterEntity = workCenterResolver.resolveApplicableToday(ruleSystemCode, workCenterCode);

        return workCenterContactRepository
                .findByWorkCenterRuleEntityIdAndContactNumber(workCenterEntity.getId(), contactNumber)
                .orElseThrow(() -> new WorkCenterContactNotFoundException(ruleSystemCode, workCenterCode, contactNumber));
    }
}