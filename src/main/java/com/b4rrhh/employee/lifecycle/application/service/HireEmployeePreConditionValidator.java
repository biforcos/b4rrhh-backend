package com.b4rrhh.employee.lifecycle.application.service;

import com.b4rrhh.employee.employee.application.service.EmployeeTypeCatalogValidator;
import com.b4rrhh.employee.employee.domain.exception.EmployeeTypeInvalidException;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.application.model.HireEmployeeDefaultValues;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeEntryReasonNotHiringException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeRequestInvalidException;
import com.b4rrhh.employee.workcenter.domain.service.WorkCenterCompanyValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class HireEmployeePreConditionValidator {

    private final WorkCenterCompanyValidator workCenterCompanyValidator;
    private final EmployeeTypeCatalogValidator employeeTypeCatalogValidator;

    public HireEmployeePreConditionValidator(
            WorkCenterCompanyValidator workCenterCompanyValidator,
            EmployeeTypeCatalogValidator employeeTypeCatalogValidator) {
        this.workCenterCompanyValidator = workCenterCompanyValidator;
        this.employeeTypeCatalogValidator = employeeTypeCatalogValidator;
    }

    public HireContext validateAndNormalize(HireEmployeeCommand command) {
        if (command == null) {
            throw new HireEmployeeRequestInvalidException("request body is required");
        }

        String ruleSystemCode = requireCode("ruleSystemCode", command.ruleSystemCode());
        String employeeTypeCode = resolveEmployeeTypeCode(command.employeeTypeCode());
        String firstName = requireText("firstName", command.firstName());
        String lastName1 = requireText("lastName1", command.lastName1());
        String lastName2 = normalizeOptionalText(command.lastName2());
        String preferredName = normalizeOptionalText(command.preferredName());
        LocalDate hireDate = requireDate(command.hireDate());

        String entryReasonCode = resolveEntryReasonCode(command.entryReasonCode());
        String companyCode = requireCode("companyCode", command.companyCode());
        String workCenterCode = requireCode("workCenterCode", command.workCenterCode());

        HireEmployeeCommand.HireEmployeeContractCommand contract = requireContract(command.contract());
        HireEmployeeCommand.HireEmployeeLaborClassificationCommand laborClassification =
                requireLaborClassification(command.laborClassification());
        HireEmployeeCommand.HireEmployeeWorkingTimeCommand workingTime = requireWorkingTime(command.workingTime());

        workCenterCompanyValidator.validateBelongsToCompany(ruleSystemCode, workCenterCode, companyCode, hireDate);

        try {
            employeeTypeCatalogValidator.validateEmployeeTypeCode(ruleSystemCode, employeeTypeCode, hireDate);
        } catch (EmployeeTypeInvalidException ex) {
            throw new HireEmployeeCatalogValueInvalidException(ex.getMessage(), ex);
        }

        return new HireContext(ruleSystemCode, employeeTypeCode, firstName, lastName1, lastName2, preferredName,
                hireDate, companyCode, entryReasonCode, workCenterCode,
                contract, laborClassification, command.costCenterDistribution(), workingTime);
    }

    // Un alta es una contratacion por construccion (b4rrhh/backend#143): sin motivo entra como
    // HIRING, con HIRING tambien, y con cualquier otro se rechaza nombrando el camino bueno.
    private String resolveEntryReasonCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return HireEmployeeDefaultValues.HIRING_ENTRY_REASON_CODE;
        }
        String code = value.trim().toUpperCase();
        if (!HireEmployeeDefaultValues.HIRING_ENTRY_REASON_CODE.equals(code)) {
            throw new HireEmployeeEntryReasonNotHiringException(code);
        }
        return code;
    }

    private String requireCode(String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new HireEmployeeRequestInvalidException(field + " is required");
        }
        return value.trim().toUpperCase();
    }

    private String requireText(String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new HireEmployeeRequestInvalidException(field + " is required");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    private LocalDate requireDate(LocalDate value) {
        if (value == null) {
            throw new HireEmployeeRequestInvalidException("hireDate is required");
        }
        return value;
    }

    private String resolveEmployeeTypeCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return HireEmployeeDefaultValues.DEFAULT_EMPLOYEE_TYPE_CODE;
        }
        return value.trim().toUpperCase();
    }

    private HireEmployeeCommand.HireEmployeeContractCommand requireContract(
            HireEmployeeCommand.HireEmployeeContractCommand contract) {
        if (contract == null) {
            throw new HireEmployeeRequestInvalidException("contract is required");
        }
        String subtypeCode = (contract.contractSubtypeCode() == null || contract.contractSubtypeCode().trim().isEmpty())
                ? null : contract.contractSubtypeCode().trim().toUpperCase();
        return new HireEmployeeCommand.HireEmployeeContractCommand(
                requireCode("contract.contractTypeCode", contract.contractTypeCode()),
                subtypeCode
        );
    }

    private HireEmployeeCommand.HireEmployeeLaborClassificationCommand requireLaborClassification(
            HireEmployeeCommand.HireEmployeeLaborClassificationCommand laborClassification) {
        if (laborClassification == null) {
            throw new HireEmployeeRequestInvalidException("laborClassification is required");
        }
        return new HireEmployeeCommand.HireEmployeeLaborClassificationCommand(
                requireCode("laborClassification.agreementCode", laborClassification.agreementCode()),
                requireCode("laborClassification.agreementCategoryCode", laborClassification.agreementCategoryCode())
        );
    }

    private HireEmployeeCommand.HireEmployeeWorkingTimeCommand requireWorkingTime(
            HireEmployeeCommand.HireEmployeeWorkingTimeCommand workingTime) {
        if (workingTime == null) {
            throw new HireEmployeeRequestInvalidException("workingTime is required");
        }
        if (workingTime.workingTimePercentage() == null) {
            throw new HireEmployeeRequestInvalidException("workingTime.workingTimePercentage is required");
        }
        return new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(
                workingTime.workingTimePercentage().stripTrailingZeros());
    }
}
