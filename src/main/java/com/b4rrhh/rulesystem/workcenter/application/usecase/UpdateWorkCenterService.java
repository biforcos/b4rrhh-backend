package com.b4rrhh.rulesystem.workcenter.application.usecase;

import com.b4rrhh.rulesystem.domain.exception.RequiredExtensionMissingException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterCatalogValidator;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterInputNormalizer;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterResolver;
import com.b4rrhh.rulesystem.workcenter.application.view.WorkCenterDetails;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenter;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterAddress;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterProfile;
import com.b4rrhh.rulesystem.workcenter.domain.port.WorkCenterProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
@Service("rulesystemUpdateWorkCenterService")
public class UpdateWorkCenterService implements UpdateWorkCenterUseCase {

    private final WorkCenterResolver workCenterResolver;
    private final WorkCenterInputNormalizer inputNormalizer;
    private final WorkCenterCatalogValidator catalogValidator;
    private final RuleEntityRepository ruleEntityRepository;
    private final WorkCenterProfileRepository workCenterProfileRepository;

    public UpdateWorkCenterService(
            WorkCenterResolver workCenterResolver,
            WorkCenterInputNormalizer inputNormalizer,
            WorkCenterCatalogValidator catalogValidator,
            RuleEntityRepository ruleEntityRepository,
            WorkCenterProfileRepository workCenterProfileRepository
    ) {
        this.workCenterResolver = workCenterResolver;
        this.inputNormalizer = inputNormalizer;
        this.catalogValidator = catalogValidator;
        this.ruleEntityRepository = ruleEntityRepository;
        this.workCenterProfileRepository = workCenterProfileRepository;
    }

    @Override
    @Transactional
        public WorkCenterDetails update(UpdateWorkCenterCommand command) {
        String ruleSystemCode = inputNormalizer.normalizeRequiredRuleSystemCode(command.ruleSystemCode());
        String workCenterCode = inputNormalizer.normalizeRequiredWorkCenterCode(command.workCenterCode());
        String name = inputNormalizer.normalizeRequiredName(command.name());
        String description = inputNormalizer.normalizeOptionalDescription(command.description());
        String companyCode = inputNormalizer.normalizeOptionalCompanyCode(command.companyCode());
        WorkCenterAddress address = inputNormalizer.normalizeAddress(
                command.street(),
                command.city(),
                command.postalCode(),
                command.regionCode(),
                command.countryCode()
        );

        RuleEntity workCenterEntity = workCenterResolver.resolveApplicableToday(ruleSystemCode, workCenterCode);
        catalogValidator.validateCompanyCode(ruleSystemCode, companyCode, LocalDate.now());
        catalogValidator.validateCountryCode(ruleSystemCode, address.getCountryCode(), LocalDate.now());

        workCenterEntity.correct(name, description, workCenterEntity.getEndDate());
        RuleEntity savedEntity = ruleEntityRepository.save(workCenterEntity);

        WorkCenterProfile existingProfile = workCenterProfileRepository.findByWorkCenterRuleEntityId(savedEntity.getId())
                .orElseThrow(() -> new RequiredExtensionMissingException(
                        savedEntity.getId(), "rulesystem.work_center_profile"));
        WorkCenterProfile savedProfile = workCenterProfileRepository.save(
                savedEntity.getId(),
                existingProfile.update(companyCode, address)
        );

        return new WorkCenterDetails(
                new WorkCenter(
                        savedEntity.getRuleSystemCode(),
                        savedEntity.getCode(),
                        savedEntity.getName(),
                        savedEntity.getDescription(),
                        savedEntity.getStartDate(),
                        savedEntity.getEndDate(),
                        savedEntity.isActive()
                ),
                savedProfile
        );
    }
}