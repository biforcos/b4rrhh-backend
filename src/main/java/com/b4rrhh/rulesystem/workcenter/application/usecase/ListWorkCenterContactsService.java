package com.b4rrhh.rulesystem.workcenter.application.usecase;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterInputNormalizer;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterResolver;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterContact;
import com.b4rrhh.rulesystem.workcenter.domain.port.WorkCenterContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListWorkCenterContactsService implements ListWorkCenterContactsUseCase {

    private final WorkCenterResolver workCenterResolver;
    private final WorkCenterInputNormalizer inputNormalizer;
    private final WorkCenterContactRepository workCenterContactRepository;

    public ListWorkCenterContactsService(
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
    public List<WorkCenterContact> list(String ruleSystemCode, String workCenterCode) {
        String normalizedRuleSystemCode = inputNormalizer.normalizeRequiredRuleSystemCode(ruleSystemCode);
        String normalizedWorkCenterCode = inputNormalizer.normalizeRequiredWorkCenterCode(workCenterCode);

        RuleEntity workCenterEntity = workCenterResolver.resolveApplicableToday(normalizedRuleSystemCode, normalizedWorkCenterCode);

        return workCenterContactRepository.findByWorkCenterRuleEntityIdOrderByContactNumberAsc(workCenterEntity.getId());
    }
}