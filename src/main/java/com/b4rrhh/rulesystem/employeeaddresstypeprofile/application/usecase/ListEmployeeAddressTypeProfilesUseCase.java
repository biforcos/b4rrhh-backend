package com.b4rrhh.rulesystem.employeeaddresstypeprofile.application.usecase;

import com.b4rrhh.rulesystem.employeeaddresstypeprofile.domain.model.EmployeeAddressTypeCoverage;

import java.util.Map;

public interface ListEmployeeAddressTypeProfilesUseCase {

    Map<String, EmployeeAddressTypeCoverage> list(String ruleSystemCode);
}
