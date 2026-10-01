package com.b4rrhh.rulesystem.catalogoption.infrastructure.persistence;

import com.b4rrhh.rulesystem.catalogoption.domain.model.WorkCenterByCompanyOption;
import com.b4rrhh.rulesystem.catalogoption.domain.port.WorkCenterByCompanyCatalogRepository;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.workcenter.infrastructure.persistence.SpringDataWorkCenterProfileRepository;
import com.b4rrhh.rulesystem.workcenter.infrastructure.persistence.WorkCenterProfileEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Los centros de una empresa: los centros los resuelve el puerto, ya filtrados y ordenados en la
 * base, y aquí sólo se quedan los que su perfil ata a la empresa, sin tocar el orden (backend#157).
 */
@Component
public class WorkCenterByCompanyCatalogReadAdapter implements WorkCenterByCompanyCatalogRepository {

    private static final String WORK_CENTER = "WORK_CENTER";

    private final RuleEntityRepository ruleEntityRepository;
    private final SpringDataWorkCenterProfileRepository springDataWorkCenterProfileRepository;

    public WorkCenterByCompanyCatalogReadAdapter(
            RuleEntityRepository ruleEntityRepository,
            SpringDataWorkCenterProfileRepository springDataWorkCenterProfileRepository
    ) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.springDataWorkCenterProfileRepository = springDataWorkCenterProfileRepository;
    }

    @Override
    public List<WorkCenterByCompanyOption> findByCompany(
            String ruleSystemCode,
            String companyCode,
            LocalDate referenceDate,
            String qLike
    ) {
        List<RuleEntity> workCenters = ruleEntityRepository
                .findActiveOptions(ruleSystemCode, WORK_CENTER, qLike, referenceDate);
        if (workCenters.isEmpty()) {
            return List.of();
        }

        Set<Long> ofTheCompany = springDataWorkCenterProfileRepository
                .findByWorkCenterRuleEntityIdIn(workCenters.stream().map(RuleEntity::getId).toList())
                .stream()
                .filter(profile -> companyCode.equals(profile.getCompanyCode()))
                .map(WorkCenterProfileEntity::getWorkCenterRuleEntityId)
                .collect(Collectors.toSet());

        return workCenters.stream()
                .filter(workCenter -> ofTheCompany.contains(workCenter.getId()))
                .map(workCenter -> new WorkCenterByCompanyOption(workCenter.getCode(), workCenter.getName()))
                .toList();
    }
}