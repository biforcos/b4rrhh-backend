package com.b4rrhh.rulesystem.workcenter.application.usecase;

import com.b4rrhh.rulesystem.domain.exception.RequiredExtensionMissingException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterInputNormalizer;
import com.b4rrhh.rulesystem.workcenter.application.view.WorkCenterDetails;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenter;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterProfile;
import com.b4rrhh.rulesystem.workcenter.domain.port.WorkCenterProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ListWorkCentersService implements ListWorkCentersUseCase {

    private final RuleEntityRepository ruleEntityRepository;
    private final WorkCenterProfileRepository workCenterProfileRepository;
    private final WorkCenterInputNormalizer inputNormalizer;

    public ListWorkCentersService(
            RuleEntityRepository ruleEntityRepository,
            WorkCenterProfileRepository workCenterProfileRepository,
            WorkCenterInputNormalizer inputNormalizer
    ) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.workCenterProfileRepository = workCenterProfileRepository;
        this.inputNormalizer = inputNormalizer;
    }

    @Override
    @Transactional(readOnly = true)
        public List<WorkCenterDetails> list(ListWorkCentersQuery query) {
        String ruleSystemCode = query.ruleSystemCode() == null
                ? null
                : inputNormalizer.normalizeRequiredRuleSystemCode(query.ruleSystemCode());

        List<RuleEntity> workCenters = ruleEntityRepository.findByFilters(
                ruleSystemCode,
                WorkCenterRuleEntityTypeCodes.WORK_CENTER,
                null,
                true,
                LocalDate.now()
        );

        Map<Long, WorkCenterProfile> profiles = workCenterProfileRepository.findByWorkCenterRuleEntityIds(
                workCenters.stream().map(RuleEntity::getId).toList()
        );

        return workCenters.stream()
                .map(entity -> {
                    WorkCenterProfile profile = profiles.get(entity.getId());
                    if (profile == null) {
                        throw new RequiredExtensionMissingException(
                                entity.getId(), "rulesystem.work_center_profile");
                    }
                    return new WorkCenterDetails(
                            new WorkCenter(
                                    entity.getRuleSystemCode(),
                                    entity.getCode(),
                                    entity.getName(),
                                    entity.getDescription(),
                                    entity.getStartDate(),
                                    entity.getEndDate(),
                                    entity.isActive()
                            ),
                            profile
                    );
                })
                .toList();
    }
}