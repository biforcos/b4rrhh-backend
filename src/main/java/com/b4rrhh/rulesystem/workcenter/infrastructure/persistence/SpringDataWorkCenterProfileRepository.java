package com.b4rrhh.rulesystem.workcenter.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataWorkCenterProfileRepository extends JpaRepository<WorkCenterProfileEntity, Long> {

    Optional<WorkCenterProfileEntity> findByWorkCenterRuleEntityId(Long workCenterRuleEntityId);

    List<WorkCenterProfileEntity> findByWorkCenterRuleEntityIdIn(Collection<Long> workCenterRuleEntityIds);
}
