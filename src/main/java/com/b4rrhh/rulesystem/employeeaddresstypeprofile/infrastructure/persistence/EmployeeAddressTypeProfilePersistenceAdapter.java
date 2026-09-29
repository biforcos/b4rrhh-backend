package com.b4rrhh.rulesystem.employeeaddresstypeprofile.infrastructure.persistence;

import com.b4rrhh.rulesystem.employeeaddresstypeprofile.domain.model.EmployeeAddressTypeCoverage;
import com.b4rrhh.rulesystem.employeeaddresstypeprofile.domain.port.EmployeeAddressTypeProfileRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class EmployeeAddressTypeProfilePersistenceAdapter implements EmployeeAddressTypeProfileRepository {

    private final SpringDataEmployeeAddressTypeProfileRepository springDataRepository;

    public EmployeeAddressTypeProfilePersistenceAdapter(
            SpringDataEmployeeAddressTypeProfileRepository springDataRepository
    ) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<EmployeeAddressTypeCoverage> findCoverageByAddressType(String ruleSystemCode, String addressTypeCode) {
        return springDataRepository
                .findCoverageByAddressType(ruleSystemCode, addressTypeCode)
                .map(EmployeeAddressTypeCoverage::valueOf);
    }

    @Override
    public Map<String, EmployeeAddressTypeCoverage> findAllCoverages(String ruleSystemCode) {
        Map<String, EmployeeAddressTypeCoverage> coverages = new LinkedHashMap<>();
        for (Object[] row : springDataRepository.findAllCoverages(ruleSystemCode)) {
            coverages.put((String) row[0], EmployeeAddressTypeCoverage.valueOf((String) row[1]));
        }
        return coverages;
    }
}
