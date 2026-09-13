package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.employee.tax_information.application.port.EmployeeForTaxInfoLookupPort;
import com.b4rrhh.employee.tax_information.infrastructure.persistence.SpringDataEmployeeTaxInformationRepository;
import com.b4rrhh.payroll.application.port.EmployeeTaxInfoContext;
import com.b4rrhh.payroll.application.port.EmployeeTaxInfoPayrollLookupPort;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class EmployeeTaxInfoPayrollLookupAdapter implements EmployeeTaxInfoPayrollLookupPort {

    private final SpringDataEmployeeTaxInformationRepository springDataRepo;
    private final EmployeeForTaxInfoLookupPort employeeLookupAdapter;

    public EmployeeTaxInfoPayrollLookupAdapter(
            SpringDataEmployeeTaxInformationRepository springDataRepo,
            EmployeeForTaxInfoLookupPort employeeLookupAdapter) {
        this.springDataRepo = springDataRepo;
        this.employeeLookupAdapter = employeeLookupAdapter;
    }

    @Override
    public EmployeeTaxInfoContext findLatestOnOrBefore(
            String ruleSystemCode, String employeeTypeCode, String employeeNumber, LocalDate referenceDate) {
        return employeeLookupAdapter.findEmployeeId(ruleSystemCode, employeeTypeCode, employeeNumber)
            .flatMap(employeeId -> springDataRepo
                .findFirstByEmployeeIdAndValidFromLessThanEqualOrderByValidFromDesc(employeeId, referenceDate))
            .map(e -> EmployeeTaxInfoContext.ofDeclared(
                e.getFamilySituation().name(),
                e.getDescendantsCount(),
                e.getAscendantsCount(),
                e.getDisabilityDegree().name(),
                e.isPensionCompensatoria(),
                e.isGeographicMobility(),
                e.isHabitualResidenceLoan(),
                e.getTaxTerritory().name()))
            .orElse(EmployeeTaxInfoContext.ofDefault());
    }
}
