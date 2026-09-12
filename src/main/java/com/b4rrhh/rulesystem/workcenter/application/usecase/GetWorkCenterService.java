package com.b4rrhh.rulesystem.workcenter.application.usecase;

import com.b4rrhh.rulesystem.domain.exception.RequiredExtensionMissingException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterInputNormalizer;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterResolver;
import com.b4rrhh.rulesystem.workcenter.application.view.WorkCenterDetails;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenter;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterProfile;
import com.b4rrhh.rulesystem.workcenter.domain.port.WorkCenterProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetWorkCenterService implements GetWorkCenterUseCase {

    private final WorkCenterResolver workCenterResolver;
    private final WorkCenterInputNormalizer inputNormalizer;
    private final WorkCenterProfileRepository workCenterProfileRepository;

    public GetWorkCenterService(
            WorkCenterResolver workCenterResolver,
            WorkCenterInputNormalizer inputNormalizer,
            WorkCenterProfileRepository workCenterProfileRepository
    ) {
        this.workCenterResolver = workCenterResolver;
        this.inputNormalizer = inputNormalizer;
        this.workCenterProfileRepository = workCenterProfileRepository;
    }

    @Override
    @Transactional(readOnly = true)
        public WorkCenterDetails get(GetWorkCenterQuery query) {
        String ruleSystemCode = inputNormalizer.normalizeRequiredRuleSystemCode(query.ruleSystemCode());
        String workCenterCode = inputNormalizer.normalizeRequiredWorkCenterCode(query.workCenterCode());

        RuleEntity workCenterEntity = workCenterResolver.resolveApplicableToday(ruleSystemCode, workCenterCode);
        WorkCenterProfile profile = workCenterProfileRepository.findByWorkCenterRuleEntityId(workCenterEntity.getId())
                .orElseThrow(() -> new RequiredExtensionMissingException(
                        workCenterEntity.getId(), "rulesystem.work_center_profile"));
        return new WorkCenterDetails(
                new WorkCenter(
                        workCenterEntity.getRuleSystemCode(),
                        workCenterEntity.getCode(),
                        workCenterEntity.getName(),
                        workCenterEntity.getDescription(),
                        workCenterEntity.getStartDate(),
                        workCenterEntity.getEndDate(),
                        workCenterEntity.isActive()
                ),
                profile
        );
    }
}