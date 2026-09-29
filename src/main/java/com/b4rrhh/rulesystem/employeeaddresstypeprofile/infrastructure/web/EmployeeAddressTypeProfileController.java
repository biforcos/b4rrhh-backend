package com.b4rrhh.rulesystem.employeeaddresstypeprofile.infrastructure.web;

import com.b4rrhh.rulesystem.employeeaddresstypeprofile.application.usecase.ListEmployeeAddressTypeProfilesUseCase;
import com.b4rrhh.rulesystem.employeeaddresstypeprofile.infrastructure.web.dto.EmployeeAddressTypeProfilesResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmployeeAddressTypeProfileController {

    private final ListEmployeeAddressTypeProfilesUseCase listUseCase;

    public EmployeeAddressTypeProfileController(ListEmployeeAddressTypeProfilesUseCase listUseCase) {
        this.listUseCase = listUseCase;
    }

    @GetMapping("/address-types/{ruleSystemCode}/profiles")
    public ResponseEntity<EmployeeAddressTypeProfilesResponse> list(@PathVariable String ruleSystemCode) {
        var items = listUseCase.list(ruleSystemCode).entrySet().stream()
                .map(entry -> new EmployeeAddressTypeProfilesResponse.Item(entry.getKey(), entry.getValue().name()))
                .toList();
        return ResponseEntity.ok(new EmployeeAddressTypeProfilesResponse(ruleSystemCode.trim().toUpperCase(), items));
    }
}
