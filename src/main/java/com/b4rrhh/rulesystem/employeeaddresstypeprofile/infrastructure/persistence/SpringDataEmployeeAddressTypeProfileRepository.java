package com.b4rrhh.rulesystem.employeeaddresstypeprofile.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpringDataEmployeeAddressTypeProfileRepository
        extends JpaRepository<EmployeeAddressTypeProfileEntity, Long> {

    /** The coverage of an address type, found through the root it hangs from (ADR-053 §1). */
    @Query("""
            select p.coverage
            from EmployeeAddressTypeProfileEntity p, RuleEntityEntity re
            where re.id = p.addressTypeRuleEntityId
              and re.ruleSystemCode = :ruleSystemCode
              and re.ruleEntityTypeCode = 'EMPLOYEE_ADDRESS_TYPE'
              and re.code = :addressTypeCode
            """)
    Optional<String> findCoverageByAddressType(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("addressTypeCode") String addressTypeCode
    );
}
