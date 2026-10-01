package com.b4rrhh.rulesystem.workcenter.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataWorkCenterProfileRepository extends JpaRepository<WorkCenterProfileEntity, Long> {

    interface WorkCenterCatalogOptionRow {
        String getCode();
        String getName();
    }

    Optional<WorkCenterProfileEntity> findByWorkCenterRuleEntityId(Long workCenterRuleEntityId);

    List<WorkCenterProfileEntity> findByWorkCenterRuleEntityIdIn(Collection<Long> workCenterRuleEntityIds);

    @Query(value = """
            select re.code as code, re.name as name
            from rulesystem.work_center_profile wcp
            join rulesystem.rule_entity re on re.id = wcp.work_center_rule_entity_id
            where re.layer_code = :ruleSystemCode
              and re.rule_entity_type_code = 'WORK_CENTER'
              and re.active = true
              and wcp.company_code = :companyCode
              and re.start_date <= :referenceDate
              and :referenceDate <= coalesce(re.end_date, date '9999-12-31')
              and (
                    :qLike is null
                    or lower(re.code) like :qLike
                    or lower(coalesce(re.name, '')) like :qLike
              )
            order by lower(coalesce(re.name, '')), re.code
            """, nativeQuery = true)
    List<WorkCenterCatalogOptionRow> findWorkCentersByRuleSystemCodeAndCompanyCode(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("companyCode") String companyCode,
            @Param("referenceDate") LocalDate referenceDate,
            @Param("qLike") String qLike
    );
}