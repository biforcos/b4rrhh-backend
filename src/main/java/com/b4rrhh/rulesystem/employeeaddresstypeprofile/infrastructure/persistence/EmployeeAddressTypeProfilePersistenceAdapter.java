package com.b4rrhh.rulesystem.employeeaddresstypeprofile.infrastructure.persistence;

import com.b4rrhh.rulesystem.employeeaddresstypeprofile.domain.model.EmployeeAddressTypeCoverage;
import com.b4rrhh.rulesystem.employeeaddresstypeprofile.domain.port.EmployeeAddressTypeProfileRepository;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La cobertura de un tipo de dirección cuelga de su raíz (ADR-053 §1); la raíz la resuelve el
 * puerto de entidades, por el nivel del tipo (backend#157).
 */
@Component
public class EmployeeAddressTypeProfilePersistenceAdapter implements EmployeeAddressTypeProfileRepository {

    private static final String EMPLOYEE_ADDRESS_TYPE = "EMPLOYEE_ADDRESS_TYPE";

    private final RuleEntityRepository ruleEntityRepository;
    private final SpringDataEmployeeAddressTypeProfileRepository springDataRepository;

    public EmployeeAddressTypeProfilePersistenceAdapter(
            RuleEntityRepository ruleEntityRepository,
            SpringDataEmployeeAddressTypeProfileRepository springDataRepository
    ) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<EmployeeAddressTypeCoverage> findCoverageByAddressType(String ruleSystemCode, String addressTypeCode) {
        return ruleEntityRepository
                .findByBusinessKey(ruleSystemCode, EMPLOYEE_ADDRESS_TYPE, addressTypeCode)
                .flatMap(addressType -> springDataRepository.findByAddressTypeRuleEntityId(addressType.getId()))
                .map(profile -> EmployeeAddressTypeCoverage.valueOf(profile.getCoverage()));
    }

    @Override
    public Map<String, EmployeeAddressTypeCoverage> findAllCoverages(String ruleSystemCode) {
        // En el orden del puerto, que es el de la base: por código.
        List<RuleEntity> addressTypes = ruleEntityRepository
                .findByFilters(ruleSystemCode, EMPLOYEE_ADDRESS_TYPE, null, null, null);
        Map<Long, EmployeeAddressTypeProfileEntity> profiles = springDataRepository
                .findByAddressTypeRuleEntityIdIn(addressTypes.stream().map(RuleEntity::getId).toList())
                .stream()
                .collect(Collectors.toMap(EmployeeAddressTypeProfileEntity::getAddressTypeRuleEntityId, Function.identity()));

        Map<String, EmployeeAddressTypeCoverage> coverages = new LinkedHashMap<>();
        for (RuleEntity addressType : addressTypes) {
            EmployeeAddressTypeProfileEntity profile = profiles.get(addressType.getId());
            if (profile != null) {
                coverages.put(addressType.getCode(), EmployeeAddressTypeCoverage.valueOf(profile.getCoverage()));
            }
        }
        return coverages;
    }
}
