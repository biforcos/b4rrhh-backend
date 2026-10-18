package com.b4rrhh.employee.photo.infrastructure.persistence;

import com.b4rrhh.employee.photo.application.port.EmployeePhotoContext;
import com.b4rrhh.employee.photo.application.port.EmployeePhotoLookupPort;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedLookupSupport;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EmployeePhotoLookupAdapter implements EmployeePhotoLookupPort {

    private final EmployeeOwnedLookupSupport employeeOwnedLookupSupport;

    public EmployeePhotoLookupAdapter(EmployeeOwnedLookupSupport employeeOwnedLookupSupport) {
        this.employeeOwnedLookupSupport = employeeOwnedLookupSupport;
    }

    @Override
    public Optional<EmployeePhotoContext> findByBusinessKey(
            String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        return employeeOwnedLookupSupport.findOwnedByBusinessKey(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                employee -> Optional.of(
                        new EmployeePhotoContext(employee.getId(), employee.getPhotoKey())
                )
        );
    }
}
