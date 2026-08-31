package com.b4rrhh.rulesystem.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataRuleEntityTypeGroupRepository extends JpaRepository<RuleEntityTypeGroupEntity, String> {

    List<RuleEntityTypeGroupEntity> findAllByOrderByDisplayOrderAsc();
}
