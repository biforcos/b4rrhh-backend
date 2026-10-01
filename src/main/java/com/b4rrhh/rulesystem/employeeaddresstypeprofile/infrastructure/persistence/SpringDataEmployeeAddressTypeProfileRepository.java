package com.b4rrhh.rulesystem.employeeaddresstypeprofile.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataEmployeeAddressTypeProfileRepository
        extends JpaRepository<EmployeeAddressTypeProfileEntity, Long> {

    Optional<EmployeeAddressTypeProfileEntity> findByAddressTypeRuleEntityId(Long addressTypeRuleEntityId);

    List<EmployeeAddressTypeProfileEntity> findByAddressTypeRuleEntityIdIn(Collection<Long> addressTypeRuleEntityIds);
}
