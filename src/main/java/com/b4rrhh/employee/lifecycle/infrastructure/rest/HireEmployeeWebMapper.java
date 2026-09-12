package com.b4rrhh.employee.lifecycle.infrastructure.rest;

import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterDistributionItemResponse;
import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterDistributionWindowResponse;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireEmployeeDefaultValues;
import com.b4rrhh.employee.lifecycle.application.model.HireEmployeeResult;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeRequestInvalidException;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.HireEmployeeRequest;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.HireEmployeeResponse;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.HireEmployeeWorkingTimeRequest;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.HiredWorkingTimeResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Component
public class HireEmployeeWebMapper {

    public HireEmployeeCommand toCommand(HireEmployeeRequest request) {
        if (request == null) {
            throw new HireEmployeeRequestInvalidException("request body is required");
        }

        return new HireEmployeeCommand(
                normalizeCode(request.ruleSystemCode()),
                normalizeEmployeeTypeCode(request.employeeTypeCode()),
                request.firstName(),
                request.lastName1(),
                request.lastName2(),
                request.preferredName(),
                request.hireDate(),
                request.entryReasonCode(),
                request.companyCode(),
                request.workCenterCode(),
                toContractCommand(request),
                toLaborClassificationCommand(request),
                request.costCenterDistribution() != null ? new HireEmployeeCommand.HireEmployeeCostCenterDistributionCommand(
                        request.costCenterDistribution().items().stream()
                                .map(item -> new HireEmployeeCommand.HireEmployeeCostCenterItemCommand(
                                        item.costCenterCode(),
                                        item.allocationPercentage()
                                ))
                                .collect(Collectors.toList())
                ) : null,
                toWorkingTimeCommand(request.workingTime())
        );
    }

    private HireEmployeeCommand.HireEmployeeContractCommand toContractCommand(HireEmployeeRequest request) {
        if (request.contract() == null) {
            throw new HireEmployeeRequestInvalidException("contract is required");
        }

        String contractTypeCode = requireText("contract.contractTypeCode", request.contract().contractTypeCode());

        return new HireEmployeeCommand.HireEmployeeContractCommand(
                contractTypeCode,
                normalizeNullableText(request.contract().contractSubtypeCode())
        );
    }

    private HireEmployeeCommand.HireEmployeeLaborClassificationCommand toLaborClassificationCommand(HireEmployeeRequest request) {
        if (request.laborClassification() == null) {
            throw new HireEmployeeRequestInvalidException("laborClassification is required");
        }

        return new HireEmployeeCommand.HireEmployeeLaborClassificationCommand(
                requireText("laborClassification.agreementCode", request.laborClassification().agreementCode()),
                requireText("laborClassification.agreementCategoryCode", request.laborClassification().agreementCategoryCode())
        );
    }

    private HireEmployeeCommand.HireEmployeeWorkingTimeCommand toWorkingTimeCommand(HireEmployeeWorkingTimeRequest request) {
        if (request == null) {
            throw new HireEmployeeRequestInvalidException("workingTime is required");
        }
        rejectDerivedHours(request);
        if (request.workingTimePercentage() == null) {
            throw new HireEmployeeRequestInvalidException("workingTime.workingTimePercentage is required");
        }

        return new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(request.workingTimePercentage());
    }

    private void rejectDerivedHours(HireEmployeeWorkingTimeRequest request) {
        if (request.weeklyHours() != null) {
            throw new HireEmployeeRequestInvalidException("workingTime.weeklyHours is not accepted");
        }
        if (request.dailyHours() != null) {
            throw new HireEmployeeRequestInvalidException("workingTime.dailyHours is not accepted");
        }
        if (request.monthlyHours() != null) {
            throw new HireEmployeeRequestInvalidException("workingTime.monthlyHours is not accepted");
        }
    }

    private String requireText(String fieldName, String value) {
        String normalized = normalizeNullableText(value);
        if (normalized == null) {
            throw new HireEmployeeRequestInvalidException(fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private String normalizeCode(String value) {
        return value != null ? value.trim().toUpperCase() : null;
    }

    private String normalizeEmployeeTypeCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return HireEmployeeDefaultValues.DEFAULT_EMPLOYEE_TYPE_CODE;
        }
        return value.trim().toUpperCase();
    }

    public HireEmployeeResponse toResponse(HireEmployeeResult result) {
        return new HireEmployeeResponse(
                result.employee().ruleSystemCode(),
                result.employee().employeeTypeCode(),
                result.employee().employeeNumber(),
                result.employee().firstName(),
                result.employee().lastName1(),
                result.employee().lastName2(),
                result.employee().preferredName(),
                result.employee().displayName(),
                result.employee().status(),
                result.employee().hireDate(),
                new HireEmployeeResponse.PresenceSummary(
                        result.presence().presenceNumber(),
                        result.presence().startDate(),
                        result.presence().companyCode(),
                        result.presence().entryReasonCode()
                ),
                new HireEmployeeResponse.WorkCenterSummary(
                        result.workCenter().startDate(),
                        result.workCenter().workCenterCode()
                ),
                result.costCenter() != null ? new CostCenterDistributionWindowResponse(
                        result.costCenter().startDate(),
                        null,
                        BigDecimal.valueOf(result.costCenter().totalAllocationPercentage()),
                        result.costCenter().items().stream()
                                .map(item -> new CostCenterDistributionItemResponse(
                                        item.costCenterCode(),
                                        null, // no enrichment at command response level
                                        BigDecimal.valueOf(item.allocationPercentage())
                                ))
                                .collect(Collectors.toList())
                ) : null,
                new HireEmployeeResponse.ContractSummary(
                        result.contract().startDate(),
                        result.contract().contractTypeCode(),
                        result.contract().contractSubtypeCode()
                ),
                new HireEmployeeResponse.LaborClassificationSummary(
                        result.laborClassification().startDate(),
                        result.laborClassification().agreementCode(),
                        result.laborClassification().agreementCategoryCode()
                ),
                new HiredWorkingTimeResponse(
                        result.workingTime().workingTimeNumber(),
                        result.workingTime().workingTimePercentage(),
                        result.workingTime().weeklyHours(),
                        result.workingTime().dailyHours(),
                        result.workingTime().monthlyHours(),
                        result.workingTime().startDate(),
                        result.workingTime().endDate()
                )
        );
    }
}
