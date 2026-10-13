package com.b4rrhh.rulesystem.workcenter.application.usecase;

import com.b4rrhh.geo.address.application.service.PostalAddressValidator;
import com.b4rrhh.geo.address.domain.model.PostalAddress;
import com.b4rrhh.rulesystem.workcenter.application.view.WorkCenterDetails;
import com.b4rrhh.rulesystem.application.usecase.NationalLayerGuard;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityTypeRepository;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterCatalogValidator;
import com.b4rrhh.rulesystem.workcenter.application.service.WorkCenterInputNormalizer;
import com.b4rrhh.rulesystem.workcenter.domain.exception.WorkCenterAlreadyExistsException;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenter;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterProfile;
import com.b4rrhh.rulesystem.workcenter.domain.port.WorkCenterProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("rulesystemCreateWorkCenterService")
public class CreateWorkCenterService implements CreateWorkCenterUseCase {

    private final RuleEntityRepository ruleEntityRepository;
    private final RuleSystemRepository ruleSystemRepository;
    private final RuleEntityTypeRepository ruleEntityTypeRepository;
    private final WorkCenterProfileRepository workCenterProfileRepository;
    private final WorkCenterInputNormalizer inputNormalizer;
    private final WorkCenterCatalogValidator catalogValidator;
    private final PostalAddressValidator postalAddressValidator;
    private final NationalLayerGuard nationalLayerGuard;

    public CreateWorkCenterService(
            RuleEntityRepository ruleEntityRepository,
            RuleSystemRepository ruleSystemRepository,
            RuleEntityTypeRepository ruleEntityTypeRepository,
            WorkCenterProfileRepository workCenterProfileRepository,
            WorkCenterInputNormalizer inputNormalizer,
            WorkCenterCatalogValidator catalogValidator,
            PostalAddressValidator postalAddressValidator,
            NationalLayerGuard nationalLayerGuard
    ) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.ruleSystemRepository = ruleSystemRepository;
        this.ruleEntityTypeRepository = ruleEntityTypeRepository;
        this.workCenterProfileRepository = workCenterProfileRepository;
        this.inputNormalizer = inputNormalizer;
        this.catalogValidator = catalogValidator;
        this.postalAddressValidator = postalAddressValidator;
        this.nationalLayerGuard = nationalLayerGuard;
    }

    @Override
    @Transactional
        public WorkCenterDetails create(CreateWorkCenterCommand command) {
        String ruleSystemCode = inputNormalizer.normalizeRequiredRuleSystemCode(command.ruleSystemCode());
        String workCenterCode = inputNormalizer.normalizeRequiredWorkCenterCode(command.workCenterCode());
        String name = inputNormalizer.normalizeRequiredName(command.name());
        String description = inputNormalizer.normalizeOptionalDescription(command.description());
        java.time.LocalDate startDate = inputNormalizer.requireStartDate(command.startDate());
        String companyCode = inputNormalizer.normalizeOptionalCompanyCode(command.companyCode());

        ruleSystemRepository.findByCode(ruleSystemCode).orElseThrow(
                () -> new IllegalArgumentException("Rule system not found with code: " + ruleSystemCode)
        );

        ruleEntityTypeRepository.findByCode(WorkCenterRuleEntityTypeCodes.WORK_CENTER).orElseThrow(
                () -> new IllegalArgumentException(
                        "Rule entity type not found with code: " + WorkCenterRuleEntityTypeCodes.WORK_CENTER
                )
        );

        nationalLayerGuard.requireNationalType(ruleSystemCode, WorkCenterRuleEntityTypeCodes.WORK_CENTER);

        ruleEntityRepository.findByBusinessKey(ruleSystemCode, WorkCenterRuleEntityTypeCodes.WORK_CENTER, workCenterCode)
                .ifPresent(existing -> {
                    throw new WorkCenterAlreadyExistsException(ruleSystemCode, workCenterCode);
                });

        catalogValidator.validateCompanyCode(ruleSystemCode, companyCode, startDate);
        // La dirección del centro es opcional entera; si viene, se valida como la del empleado
        // y la de la empresa (backend#155).
        PostalAddress address = command.address() == null
                ? null
                : postalAddressValidator.validate(ruleSystemCode, command.address(), startDate);

        RuleEntity savedEntity = ruleEntityRepository.save(new RuleEntity(
                null,
                ruleSystemCode,
                WorkCenterRuleEntityTypeCodes.WORK_CENTER,
                workCenterCode,
                name,
                description,
                true,
                startDate,
                null,
                null,
                null
        ));

        WorkCenterProfile savedProfile = workCenterProfileRepository.save(
                savedEntity.getId(),
                new WorkCenterProfile(companyCode, address)
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